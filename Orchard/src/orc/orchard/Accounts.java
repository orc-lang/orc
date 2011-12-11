//
// Accounts.java -- Java class Accounts
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

import orc.orchard.jmx.JMXUtilities;

/**
 * Provide access to accounts stored in a database.
 * @author quark
 */
public abstract class Accounts implements AccountsMBean {
	protected static Logger logger = Logger.getLogger("orc.orchard");
	
	/** Cache of Accounts objects by URL */
	private static Map<String, Accounts> urlAccounts = new HashMap<String, Accounts>();
	static {
		urlAccounts.put("", new GuestOnlyAccounts(""));
	}

	/**
	 * Get a reference to a shared Accounts object based on
	 * account information at the given URL. Currently only
	 * supports JDBC URLs but it's easy to imagine new kinds
	 * of backends.
	 */
	public static synchronized Accounts getAccounts(final String url) {
		if (urlAccounts.containsKey(url)) {
			return urlAccounts.get(url);
		} else {
			Accounts out;
			try {
				out = new DbAccounts(url, DriverManager.getConnection(url));
			} catch (final SQLException e) {
				logger.log(Level.SEVERE, "Accounts database connection failed", e);
				out = new GuestOnlyAccounts(url);
			}
			urlAccounts.put(url, out);
			return out;
		}
	}

	private final Map<Integer, Account> accounts = new HashMap<Integer, Account>();
	protected Account guest;
	private final String url;

	public Accounts(final String url) {
		this.url = url;
		guest = new GuestAccount();
		JMXUtilities.registerMBean(this, JMXUtilities.newObjectName(this, url));
		JMXUtilities.registerMBean(guest, JMXUtilities.newObjectName(guest, url + "/guest"));
	}

	private class CachedAccount extends Account {
		private final int account_id;
		public ObjectName jmxid;

		public CachedAccount(final int account_id) {
			this.account_id = account_id;
			this.jmxid = JMXUtilities.newObjectName(this, url + "/" + Integer.toString(account_id));
			JMXUtilities.registerMBean(this, jmxid);
		}

		@Override
		protected void onNoMoreJobs() {
			synchronized (Accounts.this) {
				// remove the account from the cache
				accounts.remove(account_id);
				JMXUtilities.unregisterMBean(jmxid);
			}
		}

		@Override
		public boolean getIsGuest() {
			return false;
		}
	};

	protected synchronized Account getAccount(final ResultSet rs) throws SQLException {
		final int account_id = rs.getInt("account_id");
		// Get the actual account object. If one already
		// exists, return it.
		Account out;
		if (accounts.containsKey(account_id)) {
			out = accounts.get(account_id);
		} else {
			out = new CachedAccount(account_id);
			accounts.put(account_id, out);
			out.setQuota((Integer) rs.getObject("quota"));
			out.setLifespan((Integer) rs.getObject("lifespan"));
			out.setCanSendMail(rs.getBoolean("can_send_mail"));
			out.setCanImportJava(rs.getBoolean("can_import_java"));
		}
		return out;
	}

	/**
	 * Get an account by developer key. If the developer key cannot
	 * be found, a guest account will be returned, so you can assume
	 * that this will never return null.
	 */
	public abstract Account getAccount(String devKey);

	@Override
	public synchronized Set<Integer> getAccountIDs() {
		return accounts.keySet();
	}

	@Override
	public synchronized int getNumActiveAccounts() {
		return accounts.size() + 1;
	}
}

/**
 * Accounts in the database.
 */
class DbAccounts extends Accounts {
	private final Connection db;
	private final SecureRandom random;

	public DbAccounts(final String url, final Connection db) {
		super(url);
		this.db = db;
		try {
			this.random = SecureRandom.getInstance("SHA1PRNG");
		} catch (final NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public Account getAccount(final String devKey) {
		try {
			// Check for obviously invalid accounts
			if (devKey == null || devKey.equals("")) {
				return guest;
			}
			return getAccountFromDB(devKey);
		} catch (final SQLException e) {
			// FIXME: hack to support database connection errors,
			// just return the guest account
			logger.log(Level.SEVERE, "Failed to retrieve account for devKey \""+devKey+"\"", e);
			return guest;
		}
	}

	private Account getAccountFromDB(final String devKey) throws SQLException {
		// Fetch the account information
		final PreparedStatement sql = db.prepareStatement("SELECT *" + " FROM account" + " INNER JOIN account_type USING (account_type_id)" + " WHERE developer_key = ?::uuid");
		try {
			sql.setString(1, devKey);
			final ResultSet rs = sql.executeQuery();
			try {
				if (!rs.next()) {
					// If the account was not found, use 
					// a special "guest" account.
					logger.warning("Account '" + devKey + "' not found, using guest account.");
					return guest;
				} else {
					return getAccount(rs);
				}
			} finally {
				rs.close();
			}
		} finally {
			sql.close();
		}
	}

	@Override
	public boolean changeDeveloperKey(final String username) {
		try {
			final PreparedStatement sql = db.prepareStatement("UPDATE account" + " SET developer_key = ?::uuid" + " WHERE username = ?");
			try {
				sql.setString(1, java.util.UUID.randomUUID().toString());
				sql.setString(2, username);
				sql.execute();
				return sql.getUpdateCount() > 0;
			} finally {
				sql.close();
			}
		} catch (final SQLException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public boolean changePassword(final String username, final String password) {
		try {
			final PreparedStatement sql = db.prepareStatement("UPDATE account" + " SET salt = ?, password_md5 = md5(?)" + " WHERE username = ?");
			try {
				final String salt = generateSalt();
				sql.setString(1, salt);
				sql.setString(2, salt + password);
				sql.setString(3, username);
				sql.execute();
				return sql.getUpdateCount() > 0;
			} finally {
				sql.close();
			}
		} catch (final SQLException e) {
			throw new AssertionError(e);
		}
	}

	private String generateSalt() {
		final byte[] bytes = new byte[16];
		random.nextBytes(bytes);
		return new String(bytes);
	}

	@Override
	public boolean createAccount(final int accountTypeID, final String username, final String password, final String email) {
		PreparedStatement sql;
		try {
			sql = db.prepareStatement("INSERT INTO account (account_type_id, username, salt, password_md5, developer_key, email)" + " VALUES (?, ?, ?, md5(?), ?::uuid, ?)");
		} catch (final SQLException e) {
			throw new AssertionError(e);
		}
		try {
			final String salt = generateSalt();
			sql.setInt(1, accountTypeID);
			sql.setString(2, username);
			sql.setString(3, salt);
			sql.setString(4, salt + password);
			sql.setString(5, java.util.UUID.randomUUID().toString());
			sql.setString(6, email);
			sql.execute();
			return sql.getUpdateCount() > 0;
		} catch (final SQLException e) {
			return false;
		} finally {
			try {
				sql.close();
			} catch (final SQLException e) {
				throw new AssertionError(e);
			}
		}
	}

	@Override
	public boolean dropAccount(final String username) {
		try {
			final PreparedStatement sql = db.prepareStatement("DELETE FROM account WHERE username = ?");
			try {
				sql.setString(1, username);
				sql.execute();
				return sql.getUpdateCount() > 0;
			} finally {
				sql.close();
			}
		} catch (final SQLException e) {
			throw new AssertionError(e);
		}
	}
}

/**
 * Used when there is no database.
 * @author quark
 */
class GuestOnlyAccounts extends Accounts {
	public GuestOnlyAccounts(final String url) {
		super(url);
	}

	@Override
	public Account getAccount(final String devKey) {
		return guest;
	}

	@Override
	public boolean changeDeveloperKey(final String username) {
		return false;
	}

	@Override
	public boolean changePassword(final String username, final String password) {
		return false;
	}

	@Override
	public boolean createAccount(final int accountTypeID, final String username, final String password, final String email) {
		return false;
	}

	@Override
	public boolean dropAccount(final String username) {
		return false;
	}
}

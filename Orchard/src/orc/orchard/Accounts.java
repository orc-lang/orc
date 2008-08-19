package orc.orchard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;

/**
 * Provide access to accounts stored in a database.
 * @author quark
 */
public class Accounts implements AccountsMBean {
	private final Map<Integer, Account> accounts = new HashMap<Integer, Account>();
	private Connection db;
	protected String url;
	protected Account guest = new GuestAccount();
	
	private class DbAccount extends Account {
		private int account_id;
		public ObjectName jmxid;
		public DbAccount(int account_id) {
			this.account_id = account_id;
			this.jmxid = JMXUtilities.newObjectName(this, Integer.toString(account_id));
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
		public boolean getIsGuest() { return false; }
	};
	
	public Accounts(String url) {
		this.url = url;
	}
	
	protected synchronized Account getAccount(final Integer account_id, Integer quota, Integer lifespan, Integer eventBufferSize) {
		// Get the actual account object. If one already
		// exists, return it.
		Account out;
		if (accounts.containsKey(account_id)) {
			out = accounts.get(account_id);
		} else {
			out = new DbAccount(account_id);
			accounts.put(account_id, out);
		}
		out.setQuota(quota);
		out.setLifespan(lifespan);
		out.setEventBufferSize(eventBufferSize);
		return out;
	}

	/**
	 * Get an account by developer key. If the developer key cannot
	 * be found, a guest account will be returned, so you can assume
	 * that this will never return null.
	 */
	public Account getAccount(String devKey) {
		try {
			// Check for obviously invalid accounts
			if (devKey == null || devKey.equals("")) {
				return guest;
			}
			return getAccountFromDB(devKey);
		} catch (SQLException e) {
			// FIXME: hack to support database connection errors,
			// just return the guest account
			System.err.println("SQL exception: " + e.toString());
			e.printStackTrace();
			return guest;
		}
	}

	private Account getAccountFromDB(String devKey) throws SQLException {
		if (db == null) {
			db = DriverManager.getConnection(url);
		}
		// Fetch the account information
		PreparedStatement sql = db.prepareStatement(
				"SELECT account_id, quota, lifespan, event_buffer_size" +
				" FROM account" +
				" INNER JOIN account_type USING (account_type_id)" +
				" WHERE developer_key = ?::uuid");
		try {
			sql.setString(1, devKey);
			ResultSet rs = sql.executeQuery();
			try {
				if (!rs.next()) {
					// If the account was not found, use 
					// a special "guest" account.
					System.out.println("Account '" + devKey + "' not found, using guest account.");
					return guest;
				} else {
					return getAccount((Integer)rs.getObject(1),
							(Integer)rs.getObject(2),
							(Integer)rs.getObject(3),
							(Integer)rs.getObject(4));
				}
			} finally {
				rs.close();
			}
		} finally {	
			sql.close();
		}
	}
	
	public int[] getAccountIDs() {
		Integer[] accountIDs = accounts.keySet().toArray(new Integer[]{});
		int[] out = new int[accountIDs.length];
		for (int i = 0; i < out.length; ++i) out[i] = accountIDs[i];
		return out;
	}
	
	public synchronized void finishOldJobs() {
		for (Account a : accounts.values()) a.finishOldJobs();
	}
}

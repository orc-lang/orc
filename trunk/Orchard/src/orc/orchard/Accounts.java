package orc.orchard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

/**
 * Provide access to accounts stored in a database.
 * @author quark
 */
public abstract class Accounts implements AccountsMBean {
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
	public static synchronized Accounts getAccounts(String url) {
		if (urlAccounts.containsKey(url)) {
			return urlAccounts.get(url);
		} else {
			Accounts out;
			try {
				out = new DbAccounts(url, DriverManager.getConnection(url));
			} catch (SQLException e) {
				System.err.println(e.getMessage());
				out = new GuestOnlyAccounts(url);
			}
			urlAccounts.put(url, out);
			return out;
		}
	}
	
	private final Map<Integer, Account> accounts = new HashMap<Integer, Account>();
	protected Account guest;
	private String url;
	
	public Accounts(String url) {
		this.url = url;
		guest = new GuestAccount();
		JMXUtilities.registerMBean(this,
				JMXUtilities.newObjectName(this, url));
		JMXUtilities.registerMBean(guest,
				JMXUtilities.newObjectName(guest, url+"/guest"));
	}
	
	private class CachedAccount extends Account {
		private int account_id;
		public ObjectName jmxid;
		public CachedAccount(int account_id) {
			this.account_id = account_id;
			this.jmxid = JMXUtilities.newObjectName(this,
					url+"/"+Integer.toString(account_id));
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
	
	protected synchronized Account getAccount(ResultSet rs) throws SQLException {
		int account_id = rs.getInt("account_id");
		// Get the actual account object. If one already
		// exists, return it.
		Account out;
		if (accounts.containsKey(account_id)) {
			out = accounts.get(account_id);
		} else {
			out = new CachedAccount(account_id);
			accounts.put(account_id, out);
			out.setQuota((Integer)rs.getObject("quota"));
			out.setLifespan((Integer)rs.getObject("lifespan"));
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
	
	public synchronized Set<Integer> getAccountIDs() {
		return accounts.keySet();
	}
	
	public synchronized void finishOldJobs() {
		for (Account a : accounts.values()) a.finishOldJobs();
	}

	public synchronized int getNumActiveAccounts() {
		return accounts.size() + 1;
	}
}

/**
 * Accounts in the database.
 */
class DbAccounts extends Accounts {
	private final Connection db;
	public DbAccounts(String url, Connection db) {
		super(url);
		this.db = db;
	}

	@Override
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
		// Fetch the account information
		PreparedStatement sql = db.prepareStatement(
				"SELECT *" +
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
					return getAccount(rs);
				}
			} finally {
				rs.close();
			}
		} finally {	
			sql.close();
		}
	}
}
	
/**
 * Used when there is no database.
 * @author quark
 */
class GuestOnlyAccounts extends Accounts {
	public GuestOnlyAccounts(String url) {
		super(url);
	}

	@Override
	public Account getAccount(String devKey) {
		return guest;
	}
}

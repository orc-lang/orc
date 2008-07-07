package orc.orchard.soap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.postgresql.util.PGInterval;

/**
 * Provide access to accounts stored in a database.
 * @author quark
 */
public class Accounts {
	private Map<Integer, Account> accounts = new HashMap<Integer, Account>();
	private Connection db;
	private String url;
	private Account guest;
	public Accounts(String url) {
		this.url = url;
		// create a special persistent guest account
		guest = new Account() {
			@Override
			protected void onNoMoreJobs() {}	
			public boolean isGuest() { return true; }
		};
		guest.setAge(new PGInterval(0, 0, 1, 0, 0, 0));
	}
	
	/**
	 * Get an account by developer key. If the developer key cannot
	 * be found, a guest account will be returned, so you can assume
	 * that this will never return null.
	 */
	public Account getAccount(String devKey) {
		try {
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
				"SELECT account_id, quota_all, age_all" +
				" FROM account" +
				" INNER JOIN account_type USING (account_type_id)" +
				" WHERE developer_key = ?");
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
							(PGInterval)rs.getObject(3));
				}
			} finally {
				rs.close();
			}
		} finally {	
			sql.close();
		}
	}
	
	private synchronized Account getAccount(final Integer account_id, Integer quota, PGInterval age) {
		// Get the actual account object. If one already
		// exists, return it.
		Account out;
		if (accounts.containsKey(account_id)) {
			out = accounts.get(account_id);
		} else {
			out = new Account() {
				@Override
				protected void onNoMoreJobs() {
					synchronized (Accounts.this) {
						// remove the account from the cache
						accounts.remove(account_id);
					}
				}
				public boolean isGuest() { return false; }
			};
			accounts.put(account_id, out);
		}
		out.setQuota(quota);
		out.setAge(age);
		return out;
	}
}

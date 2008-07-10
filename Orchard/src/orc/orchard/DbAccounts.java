package orc.orchard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.postgresql.util.PGInterval;

public class DbAccounts extends AbstractAccounts {
	private Connection db;
	private String url;
	public DbAccounts(String url) {
		super();
		this.url = url;
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
							(PGInterval)rs.getObject(3),
							(Integer)rs.getObject(4));
				}
			} finally {
				rs.close();
			}
		} finally {	
			sql.close();
		}
	}
}

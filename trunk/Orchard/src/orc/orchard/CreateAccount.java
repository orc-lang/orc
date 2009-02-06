package orc.orchard;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JOptionPane;

/**
 * Utility to create new accounts.
 * @author quark
 */
public class CreateAccount {
	final Connection db;
	final SecureRandom random;
	int[] accountTypeIds;
	String[] accountTypeNames;
	public CreateAccount(Connection db) throws SQLException, NoSuchAlgorithmException {
		this.db = db;
		random = SecureRandom.getInstance("SHA1PRNG");
		initAccountTypes();
	}
	
	String generateSalt() {
		byte[] bytes = new byte[16];
		random.nextBytes(bytes);
		return new String(bytes);
	}
	
	void insertAccount(int accountType, String username, String password, String email) throws SQLException {
		PreparedStatement sql = db.prepareStatement(
				"INSERT INTO account (account_type_id, username, salt, password_md5, developer_key, email)" +
				" VALUES (?, ?, ?, md5(?), ?::uuid, ?)");
		try {
			String salt = generateSalt();
			sql.setInt(1, accountTypeIds[accountType]);
			sql.setString(2, username);
			sql.setString(3, salt);
			sql.setString(4, salt + password);
			sql.setString(5, java.util.UUID.randomUUID().toString());
			sql.setString(6, email);
			sql.executeUpdate();
		} finally {	
			sql.close();
		}
	}
	
	/** Get the list of account types. */
	public void initAccountTypes() throws SQLException {
		PreparedStatement sql = db.prepareStatement(
				"SELECT account_type_id, account_type_name" +
				" FROM account_type" +
				" ORDER BY account_type_id ASC",
				ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		try {
			ResultSet rs = sql.executeQuery();
			try {
				rs.last();
				int numRows = rs.getRow();
				rs.beforeFirst();
				accountTypeIds = new int[numRows];
				accountTypeNames = new String[numRows];
				int i = 0;
				while (rs.next()) {
					accountTypeIds[i] = rs.getInt(1);
					accountTypeNames[i] = rs.getString(2);
					++i;
				}
			} finally {
				rs.close();
			}
		} finally {	
			sql.close();
		}
	}

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws SQLException, NoSuchAlgorithmException {
		CreateAccount wizard = new CreateAccount(DriverManager.getConnection(
				OrchardProperties.getProperty("orc.orchard.Accounts.url")));
		int accountType = JOptionPane.showOptionDialog(null, "Choose an account type", "Account Type",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, wizard.accountTypeNames, null);
		String username = JOptionPane.showInputDialog("Username");
		if (username == null) return;
		String password = JOptionPane.showInputDialog("Password");
		if (password == null) return;
		String email = JOptionPane.showInputDialog("E-mail");
		if (email == null) return;
		
		int ok = JOptionPane.showConfirmDialog(
			    null,
			    "Do you want to add this user account?\n\n" +
			    "Type: " + wizard.accountTypeNames[accountType] +"\n" +
	    		"Username: " + username,
			    "Confirmation",
			    JOptionPane.YES_NO_OPTION);
		if (ok != JOptionPane.YES_OPTION) return;
		
		wizard.insertAccount(accountType, username, password, email);
	}

}

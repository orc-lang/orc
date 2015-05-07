package orc.orchard;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet {
	private Connection db = null;
	@Override
	public void init() {
		try {
			String accountsUrl = OrchardProperties.getProperty(
					"orc.orchard.Accounts.url");
			if (accountsUrl.startsWith("jdbc:")) {
				db = DriverManager.getConnection(accountsUrl);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	/** Return null if dev key not found. */
	public String getDevKey(String username, String password) throws SQLException {
		if (db == null) return null;
		PreparedStatement query = db.prepareStatement(
				"SELECT developer_key FROM account" +
				" WHERE username = ? AND password_md5 = md5(salt || ?)");
		try {
			query.setString(1, username);
			query.setString(2, password);
			ResultSet result = query.executeQuery();
			try {
				if (!result.next()) return null;
				else return result.getString("developer_key");
			} finally {
				result.close();
			}
		} finally {
			query.close();
		}
	}
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String auth = request.getHeader("Authorization");
		if (auth != null) {
			String up64 = auth.substring(6);
			String up = new String(new sun.misc.BASE64Decoder().decodeBuffer(up64));
			String[] ups = up.split(":", 2);
			try {
				String devKey = getDevKey(ups[0], ups[1]);
				if (devKey != null) {
					response.sendRedirect(response.encodeRedirectURL(
							"/tryorc.shtml?k=" + devKey));
					return;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		response.setHeader("WWW-Authenticate", "Basic realm=\"orc.csres.utexas.edu\"");
		response.setStatus(401);
	}
}
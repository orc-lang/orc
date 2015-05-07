package orc.orchard;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kilim.Mailbox;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;
import orc.runtime.Kilim;
import orc.runtime.OrcEngine;

public class OrchardOAuthServlet extends HttpServlet {
	public final static String MAILBOX = "orc.orchard.OrchardOAuthServlet.MAILBOX";
	public static String getCallbackURL(OAuthAccessor accessor, Mailbox mbox, OrcEngine globals)
	throws IOException {
		accessor.setProperty(MAILBOX, mbox);
		String key = globals.addGlobal(accessor);
		// FIXME: we should figure out the callback URL
		// automatically from the servlet context
		return OAuth.addParameters(
				accessor.consumer.callbackURL,
				"k", key);
	}
	public void receiveAuthorization(HttpServletRequest request)
	throws IOException, OAuthException {
        final OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
        requestMessage.requireParameters("oauth_token", "k");
        
        OAuthAccessor accessor = (OAuthAccessor)OrcEngine.globals.remove(
        		requestMessage.getParameter("k"));
        if (accessor == null) return;
        Mailbox mbox = (Mailbox)accessor.getProperty(MAILBOX);
        if (mbox == null) return;
        System.out.println("OrchardOAuthServlet: approving " + accessor.requestToken);
        mbox.putb(Kilim.signal);
	}
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			receiveAuthorization(request);
		} catch (OAuthException e) {
			throw new ServletException(e);
		}
		PrintWriter out = response.getWriter();
		out.write("<html><head></head><body>");
		out.write("<h1>Thank you, you may now close this window.</h1>");
		out.write("</body></html>");
		out.close();
	}
}

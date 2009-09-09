package orc.orchard;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import orc.lib.orchard.MailListenerFactory;
import orc.lib.orchard.MailListenerFactory.MailListener;
import orc.runtime.OrcEngine;

/**
 * Works with {@link MailListenerFactory} to provide non-polling notification of
 * new messages.
 * 
 * <p>This should be called with a GET parameter to, whose value is the recipient
 * address where the mail was received. This is used to look up the {@link MailListener}
 * for that address and deliver the message to it.
 * 
 * @author quark
 */
@SuppressWarnings("serial")
public class MailListenerServlet extends HttpServlet {
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// initialize and check parameters
		String to = request.getQueryString();
		if (to == null) throw new ServletException("Recipient missing.");
		Object global = OrcEngine.globals.get(to);
		if (global == null) throw new ServletException("Recipient not recognized.");
		if (!(global instanceof MailListener)) throw new ServletException("Recipient not recognized.");
		
		// process the message
		MailListener receiver = (MailListener)global;
		try {
			if (!receiver.put(request.getInputStream())) {
				throw new ServletException("Inbox is full");
			}
		} catch (MessagingException e) {
			throw new ServletException(e);
		}
	}
}

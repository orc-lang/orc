/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.lib.net;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Tuple;

/**
 * Implements mail sending
 * @author wcook
 */
public class Mail extends Site {
   private static final long serialVersionUID = 1L;

	/** 
	 * Uses the java mail API to send a message via an SMTP server
	 * TODO: there are many possible enhancements of this code
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void callSite(Object[] args, Token returnToken, GroupCell caller, OrcEngine engine) {
		if (args.length != 5)
			throw new Error("sendEmail(from, to, subject, message, smtp)");

		String from = stringArg(args, 0);
		Tuple to;
		if (args[1] instanceof Tuple)
			to = (Tuple) args[1];
		else
			to = new Tuple(new Object[]{stringArg(args, 1)});
		String subject = stringArg(args, 2);
		String message = stringArg(args, 3);
		String smtp = stringArg(args, 4);

		// Set the host smtp address
		Properties props = new Properties();
		props.setProperty("mail.smtp.host", smtp);

		// create some properties and get the default Session
		Session session = Session.getDefaultInstance(props, null);
		// session.setDebug(debug);

		// create a message
		Message msg = new MimeMessage(session);

		// set the from and to address
		InternetAddress addressFrom;
		try {
			addressFrom = new InternetAddress(from);
			msg.setFrom(addressFrom);

			InternetAddress[] addressTo = new InternetAddress[to.size()];
			for (int i = 0; i < to.size(); i++) {
				addressTo[i] = new InternetAddress(to.at(i).toString());
			}
			msg.setRecipients(Message.RecipientType.TO, addressTo);

			// Optional : You can also set your custom headers in the Email
			// msg.addHeader("MyHeaderName", "myHeaderValue");

			// Setting the Subject and Content Type
			msg.setSubject(subject);
			msg.setContent(message, "text/plain");
			Transport.send(msg);
		} catch (Exception e) {
			throw new Error(e.toString());
		}
	}

}

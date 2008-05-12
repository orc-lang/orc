/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.lib.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.sites.Site;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.values.Constant;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

/**
 * Implements mail sending
 * @author wcook, quark
 */
public class Mail extends ThreadedSite {
	/** 
	 * Uses the java mail API to send a message via an SMTP server
	 * TODO: there are many possible enhancements of this code
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	@Override
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		String from, subject, message, smtp;
		TupleValue to;
		if (args.size() != 5)
			throw new OrcRuntimeTypeException("Wrong number of arguments");

		from = args.stringArg(0);

		if (args.valArg(1) instanceof TupleValue) {
			to = (TupleValue) args.valArg(1);
		} else {
			List<Value> vs = new ArrayList<Value>();
			vs.add(new Constant(args.stringArg(1)));
			to = new TupleValue(vs);
		}

		subject = args.stringArg(2);
		message = args.stringArg(3);
		smtp = args.stringArg(4);

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
			throw new OrcRuntimeTypeException(e);
		}
		return Value.signal();
	}
}
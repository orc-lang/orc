//
// MailListenerFactory.java -- Java class MailListenerFactory
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard;

import java.io.InputStream;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import kilim.Mailbox;
import kilim.Pausable;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.lib.net.MailerFactory.Mailer;
import orc.lib.net.MailerFactory.OrcMessage;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * Support for non-polling notification of new messages. Each listener has a
 * globally-unique email address ({@link MailListener#getAddress()}) and a
 * {@link MailListener#get()} method used to receive emails at that address.
 * 
 * <p>Only one process should be listening for messages at any time.
 * 
 * @author quark
 */
public class MailListenerFactory extends Site {
	public static class MailListener {
		private final Address address;
		private final Mailer mailer;
		// FIXME: should this allow multiple listeners?
		private final Mailbox<OrcMessage> inbox = new Mailbox<OrcMessage>();

		public MailListener(final OrcEngine engine, final Mailer mailer) throws AddressException {
			this.mailer = mailer;
			this.address = mailer.newFromAddress();
			OrcEngine.globals.put(engine, address.toString(), this);
		}

		public boolean put(final InputStream stream) throws MessagingException {
			return inbox.putnb(mailer.newMessage(stream));
		}

		public Address getAddress() {
			return address;
		}

		public OrcMessage get() throws Pausable {
			return inbox.get();
		}

		public void close() {
			OrcEngine.globals.remove(address.toString());
		}
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		try {
			caller.resume(new MailListener(caller.getEngine(), (Mailer) args.getArg(0)));
		} catch (final AddressException e) {
			throw new JavaException(e);
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(e);
		}
	}
}

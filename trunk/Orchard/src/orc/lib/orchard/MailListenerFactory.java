//
// MailListenerFactory.java -- Java class MailListenerFactory
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard;

import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import orc.Handle;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.lib.net.MailerFactory.Mailer;
import orc.lib.net.MailerFactory.OrcMessage;
import orc.orchard.AbstractExecutorService;
import orc.orchard.Job;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Support for non-polling notification of new messages. Each listener has a
 * globally-unique email address ({@link MailListener#getAddress()}) and a
 * {@link MailListener#get()} method used to receive emails at that address.
 * 
 * <p>Only one process should be listening for messages at any time.
 * 
 * @author quark
 */
public class MailListenerFactory extends SiteAdaptor {
	public static class MailListener {
		private final Address address;
		private final Mailer mailer;
		// TODO: should this allow multiple listeners?
		private final LinkedBlockingQueue<OrcMessage> inbox = new LinkedBlockingQueue<OrcMessage>();

		public MailListener(final Job job, final Mailer mailer) throws AddressException {
			this.mailer = mailer;
			this.address = mailer.newFromAddress();
			AbstractExecutorService.globals.put(job, address.toString(), this);
		}

		public boolean put(final InputStream stream) throws MessagingException {
			return inbox.offer(mailer.newMessage(stream));
		}

		public Address getAddress() {
			return address;
		}

		public OrcMessage get() throws InterruptedException {
			return inbox.take();
		}

		public void close() {
			AbstractExecutorService.globals.remove(address.toString());
		}
	}

	@Override
	public void callSite(final Args args, final Handle caller) throws TokenException {
		try {
			final Job job = Job.getJobFromHandle(caller);
			if (job == null) {
				caller.halt();
				return;
			}
			caller.publish(new MailListener(job, (Mailer) args.getArg(0)));
		} catch (final AddressException e) {
			throw new JavaException(e);
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(0, "orc.lib.net.MailerFactory.Mailer", args.getArg(0).getClass().getCanonicalName());
		}
	}
}

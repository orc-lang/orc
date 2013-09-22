//
// MailerFactory.java -- Java class MailerFactory
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.HeaderTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.RecipientTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import orc.Handle;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Wrapper around JavaMail API for reading and sending mail. For the most part
 * this encapsulates a subset of the <a
 * href="http://java.sun.com/products/javamail/javadocs/">JavaMail API</a>,
 * with a few key changes to simplify use in Orc:
 * <ul>
 * <li>{@link Mailer} wraps a Session object created from a properties file.
 * <li>{@link OrcMessage} provides a cooperatively-threaded wrapper for
 * blocking {@link Message} operations.
 * <li>{@link OrcFolder} provides a cooperatively-threaded wrapper for blocking
 * {@link Folder} operations.
 * <li>{@link MailFilter} provides shorthand for creating and using mail
 * filters.
 * <li>{@link OrcTransport} provides a cooperatively-threaded wrapper for
 * blocking {@link Transport} operations. It also supports a quota.
 * </ul>
 *
 * <p>This adds the following custom properties:
 * <ul>
 * <li>orc.mail.from.separator: separator used by {@link Mailer#newFromAddress()}.
 * <li>orc.mail.password: password used by default.
 * <li>orc.mail.PROTOCOL.password: password used for a specific PROTOCOL (e.g. smtp).
 * <li>orc.mail.inbox: name of inbox used by {@link OrcStore#getInbox()}.
 * <li>orc.mail.quota.duration: how long a quota period lasts
 * <li>orc.mail.quota.burst: maximum number of messages that can be sent during any one quota period
 * </ul>
 *
 * @author quark
 */
public class MailerFactory extends SiteAdaptor {
	/**
	 * Keep track of an outgoing mail quota.
	 *
	 * @author quark
	 */
	private static abstract class QuotaManager {
		/**
		 * Call before sending a message, blocking if the quota
		 * does not allow a send yet.
		 */
		public abstract void use(int count) throws InterruptedException;
	}

	/**
	 * A quota which allows sending no more than {@link #burst} mails per
	 * {@link #duration} milliseconds. Think of mails as occupying slots in
	 * a buffer of fixed size which is emptied regularly.
	 *
	 * <p>
	 * This quota scheme is somewhat unusual and hard to describe formally, but
	 * it's the closest I could come up with to something like restricted
	 * bandwidth, without tracking mail send times individually.
	 */
	private static class BurstQuotaManager extends QuotaManager {
		/** Number of burst units allowed per duration */
		private final int burst;
		/** Length of a duration in milliseconds */
		private final int duration;
		/** How many burst units will be available after {@link #endTime}? */
		private int remaining;
		/** When do the remaining burst units become available? */
		private long endTime = 0;

		public BurstQuotaManager(final int burst, final int duration) {
			this.burst = burst;
			this.duration = duration;
		}

		@Override
		public void use(final int count) throws InterruptedException {
			final long currentTime = System.currentTimeMillis();
			if (currentTime - duration > endTime) {
				// start a new burst
				endTime = currentTime;
				remaining = burst;
			}
			if (count < remaining) {
				remaining -= count;
			} else {
				throw new AssertionError("Mail quota exceeded");
			}
		}

		/* FIXME: this implementation triggers a ValidationError when Kilim-processed.
		public void use(int count) throws InterruptedException {
			int durations;
			long currentTime = System.currentTimeMillis();
			if (currentTime - duration > endTime) {
				// start a new burst
				endTime = currentTime;
				remaining = burst;
			}
			if (count < remaining) {
				// no need to use up any new durations
				remaining -= count;
				durations = 0;
			} else {
				// use up some new durations
				count -= remaining;
				remaining = burst - count % burst;
				durations = 1 + (count / burst);
			}
			// delay the end of the period
			endTime += duration * durations;
			// wait until the end of the period
			if (currentTime < endTime) {
				Task.getCurrentTask().wait(endTime - currentTime);
			}
		}
		*/
	}

	private static QuotaManager NULL_QUOTA_MANAGER = new QuotaManager() {
		@Override
		public void use(final int count) throws InterruptedException {
			// do nothing
		}
	};

	private static Map<String, QuotaManager> quotas = new HashMap<String, QuotaManager>();

	/**
	 * FIXME: currently quotas are managed per property file, globally; ideally
	 * they would be managed per engine so engines on the same VM don't
	 * interfere. In order to implement that easily, we need access to the Orc
	 * engine via Kilim. Maybe generalized quotas should be something the engine
	 * supports more directly.
	 */
	private static QuotaManager getQuotaManager(final String key, final Properties p) {
		final String burst = p.getProperty("orc.mail.quota.burst");
		if (burst == null) {
			return NULL_QUOTA_MANAGER;
		}
		final String duration = p.getProperty("orc.mail.quota.duration");
		QuotaManager out;
		synchronized (MailerFactory.class) {
			out = quotas.get(key);
			if (out == null) {
				out = new BurstQuotaManager(Integer.parseInt(burst), Integer.parseInt(duration));
				quotas.put(key, out);
			}
		}
		return out;
	}

	/**
	 * Extract the text part from the result of {@link Part#getContent()}.
	 */
	private static String findText(final Object o) throws MessagingException, IOException {
		if (o instanceof String) {
			return (String) o;
		} else if (o instanceof Multipart) {
			final Multipart m = (Multipart) o;
			for (int i = 0; i < m.getCount(); ++i) {
				final String tmp = findText(m.getBodyPart(i).getContent());
				if (tmp != null) {
					return tmp;
				}
			}
		}
		return null;
	}

	public static class Mailer {
		/** Properties for this mail site. Treated as immutable. */
		private final Properties properties;
		/** Session object. Created on-demand based on properties. */
		private Session session;
		private final QuotaManager quota;

		public Mailer(final String file) throws IOException {
			properties = new Properties();
			final InputStream stream = Mailer.class.getResourceAsStream(file);
			if (stream == null) {
				throw new FileNotFoundException(file);
			}
			properties.load(stream);
			quota = MailerFactory.getQuotaManager(file, properties);
		}

		public Session getSession() {
			if (session == null) {
				session = Session.getInstance(properties, new Authenticator() {
					/**
					 * Hack so that orc.mail.password property can be used to supply a
					 * password.
					 */
					@Override
					public PasswordAuthentication getPasswordAuthentication() {
						String password = properties.getProperty("orc.mail." + getRequestingProtocol() + ".password");
						if (password == null) {
							password = properties.getProperty("orc.mail.password");
						}
						return new PasswordAuthentication(getDefaultUserName(), password);
					}
				});
			}
			return session;
		}

		/**
		 * Convert any iterable or single thing to a list of mail addresses.
		 */
		public static Address[] toAddresses(final Object v) throws AddressException {
			final List<Address> out = new LinkedList<Address>();
			if (v instanceof Iterable<?>) {
				for (final Object x : (Iterable<?>) v) {
					out.add(new InternetAddress(x.toString()));
				}
			} else {
				out.add(new InternetAddress(v.toString()));
			}
			return out.toArray(new Address[0]);
		}

		/**
		 * Convert any iterable or single thing to a list of mail addresses.
		 */
		public static Address toAddress(final Object v) throws AddressException {
			return new InternetAddress(v.toString());
		}

		/**
		 * Get the default transport for this Mailer.
		 */
		public OrcTransport getTransport() throws NoSuchProviderException {
			return new OrcTransport(quota, getSession().getTransport());
		}

		/**
		 * Get the default store for this Mailer.
		 */
		public MailerFactory.OrcStore getStore() throws NoSuchProviderException {
			return new MailerFactory.OrcStore(getSession().getStore(), properties.getProperty("orc.mail.inbox"));
		}

		/**
		 * Create a new message.
		 */
		public OrcMessage newMessage(final String subject, final String body, final Object to) throws AddressException, MessagingException {
			final MimeMessage m = new MimeMessage(getSession());
			m.setSubject(subject);
			m.setText(body);
			m.setRecipients(Message.RecipientType.TO, toAddresses(to));
			return new OrcMessage(m);
		}

		/**
		 * Create a new message.
		 */
		public OrcMessage newMessage(final InputStream in) throws MessagingException {
			final MimeMessage m = new MimeMessage(getSession(), in);
			return new OrcMessage(m);
		}

		/**
		 * Create a new message.
		 */
		public OrcMessage newMessage() throws MessagingException {
			return new OrcMessage(new MimeMessage(getSession()));
		}

		/**
		 * Create a new MailFilter.
		 */
		public MailerFactory.MailFilter newFilter() {
			return new MailerFactory.MailFilter();
		}

		/**
		 * Generate a new random "FROM" address, of the following form:
		 * USER + orc.mail.from.separator + unique id + "@" + HOST
		 * @throws AddressException
		 */
		public Address newFromAddress() throws AddressException {
			final String from = properties.getProperty("mail.from");
			final int at = from.indexOf("@");
			if (at == -1) {
				throw new AddressException("Missing or malformed property mail.from");
			}
			final String separator = properties.getProperty("orc.mail.from.separator");
			if (separator == null) {
				throw new AddressException("Missing required property mail.from.separator");
			}
			final String uniq = java.util.UUID.randomUUID().toString();

			final String out = from.substring(0, at) + separator + uniq + from.substring(at);
			return new InternetAddress(out);
		}
	}

	/**
	 * Wrap {@link Transport} to obey quotas and use cooperative threading.
	 * @author quark
	 */
	public static class OrcTransport {
		QuotaManager quota;
		Transport transport;

		public OrcTransport(final QuotaManager quota, final Transport transport) {
			this.quota = quota;
			this.transport = transport;
		}

		public void close() throws MessagingException {
			transport.close();
		}

		public void connect() throws MessagingException {
			transport.connect();
		}

		public URLName getURLName() {
			return transport.getURLName();
		}

		public boolean isConnected() {
			return transport.isConnected();
		}

		/**
		 * Comparable to {@link Transport#send(Message)}. You should not use
		 * this on messages returned from a store, only on those created by
		 * your program (e.g. via {@link Mailer#newMessage()}).
		 */
		public void send(final OrcMessage arg0) throws MessagingException {
			arg0.message.saveChanges();
			final boolean wasConnected = transport.isConnected();
			if (!wasConnected) {
				connect();
			}
			sendMessage(arg0, arg0.message.getAllRecipients());
			if (!wasConnected) {
				close();
			}
		}

		public void sendMessage(final OrcMessage arg0, final Address[] arg1) throws MessagingException {
			try {
				quota.use(arg1.length);
			} catch (final InterruptedException e) {
				return;
			}
			transport.sendMessage(arg0.message, arg1);
		}
	}

	/**
	 * Wrap Store to use OrcFolder.
	 */
	public static class OrcStore {
		private final Store store;
		private final String inbox;

		public OrcStore(final Store store, final String inbox) {
			this.store = store;
			this.inbox = inbox;
		}

		public void connect() throws MessagingException {
			store.connect();
		}

		public void connect(final String username, final String password) throws MessagingException {
			store.connect(username, password);
		}

		public void close() throws MessagingException {
			store.close();
		}

		/**
		 * Relies on the custom property orc.mail.inbox which should be set
		 * to the name of the inbox.
		 */
		public MailerFactory.OrcFolder getInbox() throws MessagingException {
			return new MailerFactory.OrcFolder(store.getFolder(inbox));
		}

		public MailerFactory.OrcFolder getFolder(final String name) throws MessagingException {
			return new MailerFactory.OrcFolder(store.getFolder(name));
		}

		public MailerFactory.OrcFolder getDefaultFolder() throws MessagingException {
			return new MailerFactory.OrcFolder(store.getDefaultFolder());
		}

		@Override
		public String toString() {
			return super.toString() + "(" + store.toString() + ")";
		}
	}

	/**
	 * Wraps {@link Folder} to provide cooperative threading,
	 */
	public static class OrcFolder {
		private final Folder folder;

		public OrcFolder(final Folder folder) {
			this.folder = folder;
		}

		public void open() throws MessagingException {
			open(true);
		}

		public void open(final boolean write) throws MessagingException {
			folder.open(write ? Folder.READ_WRITE : Folder.READ_ONLY);
		}

		public String getName() {
			return folder.getName();
		}

		public String getFullName() {
			return folder.getFullName();
		}

		public int getMessageCount() throws MessagingException {
			return folder.getMessageCount();
		}

		public boolean exists() throws MessagingException {
			return folder.exists();
		}

		private static OrcFolder[] wrap(final Folder[] folders) {
			final OrcFolder[] foldersOut = new OrcFolder[folders.length];
			for (int i = 0; i < folders.length; ++i) {
				foldersOut[i] = new OrcFolder(folders[i]);
			}
			return foldersOut;
		}

		public OrcFolder[] list() throws MessagingException {
			return wrap(folder.list());
		}

		public OrcFolder[] list(final String name) throws MessagingException {
			return wrap(folder.list(name));
		}

		public void close() throws MessagingException {
			close(true);
		}

		public void close(final boolean expunge) throws MessagingException {
			folder.close(expunge);
		}

		public OrcMessage[] getMessages() throws MessagingException {
			return OrcMessage.wrap(folder.getMessages());
		}

		@Override
		public String toString() {
			return super.toString() + "(" + folder.toString() + ")";
		}
	}

	/**
	 * Wraps methods of {@link Message} to make them use cooperative
	 * threading and return {@link OrcFolder} when appropriate. Also
	 * added utility methods {@link #getText()} and {@link #delete()}.
	 *
	 * @author quark
	 */
	public static class OrcMessage {
		private final Message message;

		private static OrcMessage[] wrap(final Message[] messages) {
			final OrcMessage[] messagesOut = new OrcMessage[messages.length];
			for (int i = 0; i < messages.length; ++i) {
				messagesOut[i] = new OrcMessage(messages[i]);
			}
			return messagesOut;
		}

		public OrcMessage(final Message message) {
			this.message = message;
		}

		public void addFrom(final Address[] arg0) throws MessagingException {
			message.addFrom(arg0);
		}

		public void addHeader(final String arg0, final String arg1) throws MessagingException {
			message.addHeader(arg0, arg1);
		}

		public void addRecipient(final RecipientType arg0, final Address arg1) throws MessagingException {
			message.addRecipient(arg0, arg1);
		}

		public void addRecipients(final RecipientType arg0, final Address[] arg1) throws MessagingException {
			message.addRecipients(arg0, arg1);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return message == null ? 1 : message.hashCode();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof OrcMessage)) {
				return false;
			}
			final OrcMessage other = (OrcMessage) obj;
			if (message == null) {
				if (other.message != null) {
					return false;
				}
			} else if (!message.equals(other.message)) {
				return false;
			}
			return true;
		}

		public Enumeration<?> getAllHeaders() throws MessagingException {
			return message.getAllHeaders();
		}

		public Address[] getAllRecipients() throws MessagingException {
			return message.getAllRecipients();
		}

		public OrcFolder getFolder() {
			return new OrcFolder(message.getFolder());
		}

		public Object getContent() throws IOException, MessagingException {
			try {
				return message.getContent();
			} catch (final Exception e) {
				// HACK: for some reason when I put these
				// as separate catch clauses it doesn't work like I expect
				if (e instanceof MessagingException) {
					throw (MessagingException) e;
				} else if (e instanceof IOException) {
					throw (IOException) e;
				} else if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				} else {
					throw new AssertionError(e);
				}
			}
		}

		public Address[] getFrom() throws MessagingException {
			return message.getFrom();
		}

		public String[] getHeader(final String arg0) throws MessagingException {
			return message.getHeader(arg0);
		}

		public int getLineCount() throws MessagingException {
			return message.getLineCount();
		}

		public Date getReceivedDate() throws MessagingException {
			return message.getReceivedDate();
		}

		public Address[] getRecipients(final RecipientType arg0) throws MessagingException {
			return message.getRecipients(arg0);
		}

		public Address[] getReplyTo() throws MessagingException {
			return message.getReplyTo();
		}

		public Date getSentDate() throws MessagingException {
			return message.getSentDate();
		}

		public int getSize() throws MessagingException {
			return message.getSize();
		}

		public String getSubject() throws MessagingException {
			return message.getSubject();
		}

		public boolean isExpunged() {
			return message.isExpunged();
		}

		public boolean isMimeType(final String arg0) throws MessagingException {
			return message.isMimeType(arg0);
		}

		public void removeHeader(final String arg0) throws MessagingException {
			message.removeHeader(arg0);
		}

		public OrcMessage autoReply(final boolean replyToAll) throws MessagingException {
			if (!safeToAutoRespond()) {
				return null;
			}
			OrcMessage reply = new OrcMessage(message.reply(replyToAll));
			reply.addHeader("Auto-Submitted", "auto-replied");
			String subject = reply.getSubject();
			if (subject.regionMatches(true, 0, "Re: ", 0, 4)) subject = subject.substring(4);
			reply.setSubject("Auto: "+subject);
			return reply;
		}

		public OrcMessage humanReply(final boolean replyToAll) throws MessagingException {
			return new OrcMessage(message.reply(replyToAll));
		}

		public void saveChanges() throws MessagingException {
			message.saveChanges();
		}

		public void setContent(final Multipart arg0) throws MessagingException {
			message.setContent(arg0);
		}

		public void setContent(final Object arg0, final String arg1) throws MessagingException {
			message.setContent(arg0, arg1);
		}

		public void setFrom(final Address arg0) throws MessagingException {
			message.setFrom(arg0);
		}

		public void setHeader(final String arg0, final String arg1) throws MessagingException {
			message.setHeader(arg0, arg1);
		}

		public void setRecipient(final RecipientType arg0, final Address arg1) throws MessagingException {
			message.setRecipient(arg0, arg1);
		}

		public void setRecipients(final RecipientType arg0, final Address[] arg1) throws MessagingException {
			message.setRecipients(arg0, arg1);
		}

		public void setReplyTo(final Address[] arg0) throws MessagingException {
			message.setReplyTo(arg0);
		}

		public void setSentDate(final Date arg0) throws MessagingException {
			message.setSentDate(arg0);
		}

		public void setSubject(final String arg0) throws MessagingException {
			message.setSubject(arg0);
		}

		public void setText(final String arg0) throws MessagingException {
			message.setText(arg0);
		}

		/** Added utility method. */
		public String getText() throws IOException, MessagingException {
			return findText(getContent());
		}

		/** Added utility method. */
		public void delete() throws MessagingException {
			message.setFlag(Flags.Flag.DELETED, true);
		}

		/** Check if auto-reply to this message is allowable per RFC 3834 */
		public boolean safeToAutoRespond() throws MessagingException {
			String[] autoSubmittedHdrs = message.getHeader("Auto-Submitted");
			String[] returnPathHdrs = message.getHeader("Return-Path");
			Address[] replyTo = message.getReplyTo();
			String[] precedenceHdrs = message.getHeader("Precedence");
			return
				// No Auto-Submitted header, or Auto-Submitted: no
				(autoSubmittedHdrs.length == 0 || (autoSubmittedHdrs.length == 1 && autoSubmittedHdrs[0].equalsIgnoreCase("no"))) &&
				// No Return-Path: <>
				(returnPathHdrs.length == 0 || (returnPathHdrs.length == 1 && !returnPathHdrs[0].equalsIgnoreCase("<>"))) &&
				(replyTo != null && replyTo.length > 0 && !replyTo[0].toString().toUpperCase().contains("MAILER-DAEMON@")) &&
				// TODO: Maybe? replyTo is not "owner-*", "*-request"
				// TODO: Maybe? Does not have any List-* headers
				// No Precedence: list, junk, or bulk
				(precedenceHdrs.length == 0 || (precedenceHdrs.length == 1 && !precedenceHdrs[0].equalsIgnoreCase("list") && !precedenceHdrs[0].equalsIgnoreCase("junk") && !precedenceHdrs[0].equalsIgnoreCase("bulk")));
		}
	}

	/**
	 * Filter for a MailFolder. Methods on a filter return a new filter, so you
	 * can create compound filters easily. I.e. to filter mails from "foo" with
	 * subject "bar": filter.from("foo").subject("bar")
	 *
	 * <p>
	 * Filters include:
	 * <ul>
	 * <li>not(): negate filter
	 * <li>and(MailFilter): intersect with another filter
	 * <li>or(MailFilter): union with another filter
	 * <li>from(Address): match a (complete) from address
	 * <li>to(Address): match a (complete) to address
	 * <li>cc(Address): match a (complete) cc address
	 * <li>bcc(Address): match a (complete) bcc address
	 * <li>recipient(Address): match any complete to/cc/bcc address
	 * <li>subject(String): match any substring of the subject
	 * <li>body(String): match any substring of the body
	 * <li>header(header, content): match any substring of a header
	 * </ul>
	 *
	 * @author quark
	 *
	 */
	public static class MailFilter {
		private final SearchTerm term;

		public MailFilter(final SearchTerm term) {
			this.term = term;
		}

		public MailFilter() {
			this(null);
		}

		private MailFilter filter(final SearchTerm otherTerm) {
			return new MailFilter(term == null ? otherTerm : new AndTerm(term, otherTerm));
		}

		public OrcMessage[] search(final OrcFolder folder) throws MessagingException {
			if (term == null) {
				return OrcMessage.wrap(folder.folder.getMessages());
			} else {
				return OrcMessage.wrap(folder.folder.search(term));
			}
		}

		public MailFilter and(final MailFilter that) {
			return filter(that.term);
		}

		public MailFilter or(final MailFilter that) {
			return new MailFilter(term == null ? that.term : new OrTerm(term, that.term));
		}

		public MailFilter not() {
			return new MailFilter(new NotTerm(term == null
			// this always matches
			? new FlagTerm(new Flags(), false)
					: term));
		}

		public MailFilter from(final String address) throws AddressException {
			return filter(new FromTerm(new InternetAddress(address)));
		}

		public MailFilter to(final String address) throws AddressException {
			return filter(new RecipientTerm(Message.RecipientType.TO, new InternetAddress(address)));
		}

		public MailFilter cc(final String address) throws AddressException {
			return filter(new RecipientTerm(Message.RecipientType.CC, new InternetAddress(address)));
		}

		public MailFilter bcc(final String address) throws AddressException {
			return filter(new RecipientTerm(Message.RecipientType.BCC, new InternetAddress(address)));
		}

		public MailFilter recipient(final String address0) throws AddressException {
			final InternetAddress address = new InternetAddress(address0);
			return filter(new OrTerm(new SearchTerm[] { new RecipientTerm(Message.RecipientType.TO, address), new RecipientTerm(Message.RecipientType.CC, address), new RecipientTerm(Message.RecipientType.BCC, address) }));
		}

		public MailFilter body(final String body) {
			return filter(new BodyTerm(body));
		}

		public MailFilter subject(final String body) {
			return filter(new SubjectTerm(body));
		}

		public MailFilter header(final String name, final String value) {
			return filter(new HeaderTerm(name, value));
		}
	}

	@Override
	public void callSite(final Args args, final Handle caller) throws TokenException {
		requireRight(caller, "send mail");
		try {
			caller.publish(new Mailer("/" + args.stringArg(0)));
		} catch (final IOException e) {
			throw new JavaException(e);
		}
	}
}

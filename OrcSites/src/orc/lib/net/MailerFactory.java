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
import java.util.concurrent.Callable;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.Message.RecipientType;
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

import kilim.Fiber;
import kilim.Pausable;
import orc.error.OrcError;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.sites.Site;

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
public class MailerFactory extends Site {
	/**
	 * Keep track of an outgoing mail quota.
	 * FIXME: Kilim incorrectly adds methods to an interface
	 * @author quark
	 */
	private static abstract class QuotaManager {
		/**
		 * Call before sending a message, blocking if the quota
		 * does not allow a send yet.
		 */
		public abstract void use(int count) throws Pausable, InterruptedException;
		/** FIXME: Kilim should add this method but it doesn't */
		public void use(int count, Fiber f) throws InterruptedException {
			throw new AssertionError("Unwoven method "
					+ this.getClass().toString()
					+ "#use(int)");
		}
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
		private int burst;
		/** Length of a duration in milliseconds */
		private int duration;
		/** How many burst units will be available after {@link #endTime}? */
		private int remaining;
		/** When do the remaining burst units become available? */
		private long endTime = 0;
		public BurstQuotaManager(int burst, int duration) {
			this.burst = burst;
			this.duration = duration;
		}
		
		public void use(int count) throws Pausable, InterruptedException {
			long currentTime = System.currentTimeMillis();
			if (currentTime - duration > endTime) {
				// start a new burst
				endTime = currentTime;
				remaining = burst;
			}
			if (count < remaining) {
				remaining -= count;
			} else {
				throw new OrcError("Mail quota exceeded");
			}
		}
		
		/* FIXME: this implementation triggers a ValidationError when Kilim-processed.
		public void use(int count) throws Pausable, InterruptedException {
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
		public void use(int count) throws Pausable, InterruptedException {
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
	private static QuotaManager getQuotaManager(String key, Properties p) {
		String burst = p.getProperty("orc.mail.quota.burst");
		if (burst == null) return NULL_QUOTA_MANAGER;
		String duration = p.getProperty("orc.mail.quota.duration");
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
	 * Utility method to run a blocking mail operation.
	 */
	private static <E> E runThreaded(Callable<E> thunk) throws MessagingException, Pausable {
		try {
			return Kilim.runThreaded(thunk);
		} catch (Exception e) {
			// HACK: for some reason when I put these
			// as separate catch clauses it doesn't work like I expect
			if (e instanceof MessagingException) {
				throw (MessagingException)e;
			} else if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			} else {
				throw new AssertionError(e);
			}
		}
	}

	/**
	 * Extract the text part from the result of {@link Part#getContent()}.
	 */
	private static String findText(Object o) throws MessagingException, IOException {
		if (o instanceof String) {
			return (String) o;
		} else if (o instanceof Multipart) {
			Multipart m = (Multipart) o;
			for (int i = 0; i < m.getCount(); ++i) {
				String tmp = findText(m.getBodyPart(i).getContent());
				if (tmp != null)
					return tmp;
			}
		}
		return null;
	}
	
	public static class Mailer {
		/** Properties for this mail site. Treated as immutable. */
		private Properties properties;
		/** Session object. Created on-demand based on properties. */
		private Session session;
		private QuotaManager quota;
		
		public Mailer(String file) throws IOException {
			properties = new Properties();
			InputStream stream = Mailer.class.getResourceAsStream(file);
			if (stream == null) throw new FileNotFoundException(file);
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
					public PasswordAuthentication getPasswordAuthentication() {
						String password = properties.getProperty("orc.mail."
								+ getRequestingProtocol() + ".password");
						if (password == null) {
							password = properties.getProperty("orc.mail.password");
						}
						return new PasswordAuthentication(getDefaultUserName(),
								password);
					}
				});
			}
			return session;
		}
	
		/**
		 * Convert any iterable or single thing to a list of mail addresses.
		 */
		public static Address[] toAddresses(Object v) throws AddressException {
			List<Address> out = new LinkedList<Address>();
			if (v instanceof Iterable) {
				for (Object x : (Iterable)v) {
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
		public static Address toAddress(Object v) throws AddressException {
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
			return new MailerFactory.OrcStore(getSession().getStore(),
				properties.getProperty("orc.mail.inbox"));
		}
		/**
		 * Create a new message.
		 */
		public OrcMessage newMessage(String subject, String body, Object to) throws AddressException, MessagingException {
			MimeMessage m = new MimeMessage(getSession());
			m.setSubject(subject);
			m.setText(body);
			m.setRecipients(Message.RecipientType.TO, toAddresses(to));
			return new OrcMessage(m);
		}
		/**
		 * Create a new message.
		 */
		public OrcMessage newMessage(InputStream in) throws MessagingException {
			MimeMessage m = new MimeMessage(getSession(), in);
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
			String from = properties.getProperty("mail.from");
			int at = from.indexOf("@");
			if (at == -1) throw new AddressException("Missing or malformed property mail.from");
			String separator = properties.getProperty("orc.mail.from.separator");
			if (separator == null) throw new AddressException("Missing required property mail.from.separator");
			String uniq = java.util.UUID.randomUUID().toString();
			
			String out = from.substring(0, at) + separator + uniq + from.substring(at);
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
		public OrcTransport(QuotaManager quota, Transport transport) {
			this.quota = quota;
			this.transport = transport;
		}
		public void close() throws MessagingException, Pausable {
			runThreaded(new Callable<Void>() {
				public Void call() throws MessagingException {
					transport.close();
					return null;
				}
			});
		}
		public void connect() throws MessagingException, Pausable {
			runThreaded(new Callable<Void>() {
				public Void call() throws MessagingException {
					transport.connect();
					return null;
				}
			});
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
		public void send(OrcMessage arg0) throws MessagingException, Pausable {
			arg0.message.saveChanges();
			boolean wasConnected = transport.isConnected();
			if (!wasConnected) connect();
			sendMessage(arg0, arg0.message.getAllRecipients());
			if (!wasConnected) close();
		}
		public void sendMessage(final OrcMessage arg0, final Address[] arg1) throws MessagingException, Pausable {
			try {
				quota.use(arg1.length);
			} catch (InterruptedException e) {
				return;
			}
			runThreaded(new Callable<Void>() {
				public Void call() throws MessagingException {
					transport.sendMessage(arg0.message, arg1);
					return null;
				}
			});
		}
	}

	/**
	 * Wrap Store to use OrcFolder.
	 */
	public static class OrcStore {
		private Store store;
		private String inbox;
	
		public OrcStore(Store store, String inbox) {
			this.store = store;
			this.inbox = inbox;
		}
		
		public void connect() throws MessagingException {
			store.connect();
		}
		
		public void connect(String username, String password) throws MessagingException {
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
	
		public MailerFactory.OrcFolder getFolder(String name) throws MessagingException {
			return new MailerFactory.OrcFolder(store.getFolder(name));
		}
		
		public MailerFactory.OrcFolder getDefaultFolder() throws MessagingException {
			return new MailerFactory.OrcFolder(store.getDefaultFolder());
		}
	
		public String toString() {
			return super.toString() + "(" + store.toString() + ")";
		}
	}

	/**
	 * Wraps {@link Folder} to provide cooperative threading,
	 */
	public static class OrcFolder {
		private Folder folder;
	
		public OrcFolder(Folder folder) {
			this.folder = folder;
		}
		
		public void open() throws MessagingException, Pausable {
			open(true);
		}
		
		public void open(final boolean write) throws MessagingException, Pausable {
			runThreaded(new Callable<Void>() {
				public Void call() throws MessagingException {
					folder.open(write ? Folder.READ_WRITE : Folder.READ_ONLY);
					return null;
				}
			});
		}
		
		public String getName() {
			return folder.getName();
		}
		
		public String getFullName() {
			return folder.getFullName();
		}
		
		public int getMessageCount() throws MessagingException, Pausable {
			return runThreaded(new Callable<Integer>() {
				public Integer call() throws MessagingException {
					return folder.getMessageCount();
				}
			});
		}
		
		public boolean exists() throws MessagingException, Pausable {
			return runThreaded(new Callable<Boolean>() {
				public Boolean call() throws MessagingException {
					return folder.exists();
				}
			});
		}
		
		private static OrcFolder[] wrap(Folder[] folders) {
			OrcFolder[] foldersOut = new OrcFolder[folders.length];
			for (int i = 0; i < folders.length; ++i) {
				foldersOut[i] = new OrcFolder(folders[i]);
			}
			return foldersOut;
		}
		
		public OrcFolder[] list() throws MessagingException, Pausable {
			return runThreaded(new Callable<OrcFolder[]>() {
				public OrcFolder[] call() throws MessagingException {
					return wrap(folder.list());
				}
			});
		}
		
		public OrcFolder[] list(final String name) throws MessagingException, Pausable {
			return runThreaded(new Callable<OrcFolder[]>() {
				public OrcFolder[] call() throws MessagingException {
					return wrap(folder.list(name));
				}
			});
		}
		
		public void close() throws MessagingException, Pausable {
			close(true);
		}
		
		public void close(final boolean expunge) throws MessagingException, Pausable {
			runThreaded(new Callable<Void>() {
				public Void call() throws MessagingException {
					folder.close(expunge);
					return null;
				}
			});
		}
		
		public OrcMessage[] getMessages() throws MessagingException, Pausable {
			return runThreaded(new Callable<OrcMessage[]>() {
				public OrcMessage[] call() throws MessagingException {
					return OrcMessage.wrap(folder.getMessages());
				}
			});
		}
	
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
		private Message message;
		
		private static OrcMessage[] wrap(Message[] messages) {
			OrcMessage[] messagesOut = new OrcMessage[messages.length];
			for (int i = 0; i < messages.length; ++i) {
				messagesOut[i] = new OrcMessage(messages[i]);
			}
			return messagesOut;
		}
	
		public OrcMessage(Message message) {
			this.message = message;
		}
	
		public void addFrom(Address[] arg0) throws MessagingException {
			message.addFrom(arg0);
		}
	
		public void addHeader(String arg0, String arg1) throws MessagingException {
			message.addHeader(arg0, arg1);
		}
	
		public void addRecipient(RecipientType arg0, Address arg1) throws MessagingException {
			message.addRecipient(arg0, arg1);
		}
	
		public void addRecipients(RecipientType arg0, Address[] arg1) throws MessagingException {
			message.addRecipients(arg0, arg1);
		}
	
		public boolean equals(Object arg0) {
			if (arg0 instanceof OrcMessage) {
				return message.equals(((OrcMessage)arg0).message);
			} else return false;
		}
	
		public Enumeration getAllHeaders() throws MessagingException, Pausable {
			return runThreaded(new Callable<Enumeration>() {
				public Enumeration call() throws MessagingException {
					return message.getAllHeaders();
				}
			});
		}
	
		public Address[] getAllRecipients() throws MessagingException, Pausable {
			return runThreaded(new Callable<Address[]>() {
				public Address[] call() throws MessagingException {
					return message.getAllRecipients();
				}
			});
		}
	
		public OrcFolder getFolder() {
			return new OrcFolder(message.getFolder());
		}
	
		public Object getContent() throws IOException, MessagingException, Pausable {
			try {
				return Kilim.runThreaded(new Callable<Object>() {
					public Object call() throws IOException, MessagingException {
						return message.getContent();
					}
				});
			} catch (Exception e) {
				// HACK: for some reason when I put these
				// as separate catch clauses it doesn't work like I expect
				if (e instanceof MessagingException) {
					throw (MessagingException)e;
				} else if (e instanceof IOException) {
					throw (IOException)e;
				} else if (e instanceof RuntimeException) {
					throw (RuntimeException)e;
				} else {
					throw new AssertionError(e);
				}
			}
		}
	
		public Address[] getFrom() throws MessagingException, Pausable {
			return runThreaded(new Callable<Address[]>() {
				public Address[] call() throws MessagingException {
					return message.getFrom();
				}
			});
		}
	
		public String[] getHeader(final String arg0) throws MessagingException, Pausable {
			return runThreaded(new Callable<String[]>() {
				public String[] call() throws MessagingException {
					return message.getHeader(arg0);
				}
			});
		}
	
		public int getLineCount() throws MessagingException, Pausable {
			return runThreaded(new Callable<Integer>() {
				public Integer call() throws MessagingException {
					return message.getLineCount();
				}
			});
		}
	
		public Date getReceivedDate() throws MessagingException, Pausable {
			return runThreaded(new Callable<Date>() {
				public Date call() throws MessagingException {
					return message.getReceivedDate();
				}
			});
		}
	
		public Address[] getRecipients(final RecipientType arg0) throws MessagingException, Pausable {
			return runThreaded(new Callable<Address[]>() {
				public Address[] call() throws MessagingException {
					return message.getRecipients(arg0);
				}
			});
		}
	
		public Address[] getReplyTo() throws MessagingException, Pausable {
			return runThreaded(new Callable<Address[]>() {
				public Address[] call() throws MessagingException {
					return message.getReplyTo();
				}
			});
		}
	
		public Date getSentDate() throws MessagingException, Pausable {
			return runThreaded(new Callable<Date>() {
				public Date call() throws MessagingException {
					return message.getSentDate();
				}
			});
		}
	
		public int getSize() throws MessagingException {
			return message.getSize();
		}
	
		public String getSubject() throws MessagingException, Pausable {
			return runThreaded(new Callable<String>() {
				public String call() throws MessagingException {
					return message.getSubject();
				}
			});
		}
	
		public boolean isExpunged() {
			return message.isExpunged();
		}
	
		public boolean isMimeType(String arg0) throws MessagingException {
			return message.isMimeType(arg0);
		}
	
		public void removeHeader(String arg0) throws MessagingException {
			message.removeHeader(arg0);
		}
	
		public OrcMessage reply(final boolean arg0) throws MessagingException, Pausable {
			return runThreaded(new Callable<OrcMessage>() {
				public OrcMessage call() throws MessagingException {
					return new OrcMessage(message.reply(arg0));
				}
			});
		}
	
		public void saveChanges() throws MessagingException {
			message.saveChanges();
		}
	
		public void setContent(Multipart arg0) throws MessagingException {
			message.setContent(arg0);
		}
	
		public void setContent(Object arg0, String arg1) throws MessagingException {
			message.setContent(arg0, arg1);
		}
	
		public void setFrom(Address arg0) throws MessagingException {
			message.setFrom(arg0);
		}
	
		public void setHeader(String arg0, String arg1) throws MessagingException {
			message.setHeader(arg0, arg1);
		}
	
		public void setRecipient(RecipientType arg0, Address arg1) throws MessagingException {
			message.setRecipient(arg0, arg1);
		}
	
		public void setRecipients(RecipientType arg0, Address[] arg1) throws MessagingException {
			message.setRecipients(arg0, arg1);
		}
	
		public void setReplyTo(Address[] arg0) throws MessagingException {
			message.setReplyTo(arg0);
		}
	
		public void setSentDate(Date arg0) throws MessagingException {
			message.setSentDate(arg0);
		}
	
		public void setSubject(String arg0) throws MessagingException {
			message.setSubject(arg0);
		}
	
		public void setText(String arg0) throws MessagingException {
			message.setText(arg0);
		}
		
		/** Added utility method. */
		public String getText() throws IOException, MessagingException, Pausable {
			return findText(getContent());
		}
		
		/** Added utility method. */
		public void delete() throws MessagingException {
			message.setFlag(Flags.Flag.DELETED, true);
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
		private SearchTerm term;
	
		public MailFilter(SearchTerm term) {
			this.term = term;
		}
	
		public MailFilter() {
			this(null);
		}
	
		private MailFilter filter(SearchTerm otherTerm) {
			return new MailFilter(term == null ? otherTerm : new AndTerm(term,
					otherTerm));
		}
	
		public OrcMessage[] search(final OrcFolder folder) throws MessagingException, Pausable {
			return runThreaded(new Callable<OrcMessage[]>() {
				public OrcMessage[] call() throws MessagingException {
					if (term == null) {
						return OrcMessage.wrap(folder.folder.getMessages());
					} else {
						return OrcMessage.wrap(folder.folder.search(term));
					}
				}
			});
		}
		
		public MailFilter and(MailFilter that) {
			return filter(that.term);
		}
		
		public MailFilter or(MailFilter that) {
			return new MailFilter(term == null
					? that.term
					: new OrTerm(term, that.term));
		}
		
		public MailFilter not() {
			return new MailFilter(
					new NotTerm(term == null
							// this always matches
							? new FlagTerm(new Flags(), false)
							: term));
		}
		
		public MailFilter from(String address) throws AddressException {
			return filter(new FromTerm(new InternetAddress(address)));
		}
		
		public MailFilter to(String address) throws AddressException {
			return filter(new RecipientTerm(
					Message.RecipientType.TO,
					new InternetAddress(address)));
		}
		
		public MailFilter cc(String address) throws AddressException {
			return filter(new RecipientTerm(
					Message.RecipientType.CC,
					new InternetAddress(address)));
		}
		
		public MailFilter bcc(String address) throws AddressException {
			return filter(new RecipientTerm(
					Message.RecipientType.BCC,
					new InternetAddress(address)));
		}
		
		public MailFilter recipient(String address0) throws AddressException {
			InternetAddress address = new InternetAddress(address0);
			return filter(new OrTerm(new SearchTerm[] {
					new RecipientTerm(Message.RecipientType.TO,
							address),
					new RecipientTerm(Message.RecipientType.CC,
							address),
					new RecipientTerm(Message.RecipientType.BCC,
							address) }));
		}
		
		public MailFilter body(String body) {
				return filter(new BodyTerm(body));
		}
		
		public MailFilter subject(String body) {
				return filter(new SubjectTerm(body));
		}
		
		public MailFilter header(String name, String value) {
				return filter(new HeaderTerm(name, value));
		}
	}

	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		caller.requireCapability("send mail", true);
		try {
			caller.resume(new Mailer("/" + args.stringArg(0)));
		} catch (IOException e) {
			throw new JavaException(e);
		}
	}
}

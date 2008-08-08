package orc.lib.net;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
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

import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.values.Value;

/**
 * Wrapper around JavaMail API for reading and sending mail. For the most part
 * this follows a subset of the JavaMail API, except some methods are renamed or
 * tweaked to be slightly more idiomatic Orc and to avoid having to create too
 * many temporary objects.
 * 
 * Main methods:
 * <ul>
 * <li>message(?subject, ?body, ?to, ?from): create a new MailMessage
 * <li>transport(?url): create a new outgoing mail service (MailTransport)
 * <li>store(?url): create a new mail repository service (MailStore)
 * <li>withProperties(name, value, ...): set one or more properties; see
 * JavaMail javadoc for applicable properties and settings. "user.password" and
 * "user.PROTOCOL.password" are undocumented properties which will work.
 * <li>property(name): get the value of a property
 * </ul>
 * 
 * @author quark
 */
public class Mail extends DotSite {
	/** Properties for this mail site. Treated as immutable. */
	private Properties properties;

	/** Session object. Created on-demand based on properties. */
	private Session session;

	public Mail() {
		// this(System.getProperties());
		this(new Properties());
	}

	private Mail(Properties properties) {
		this.properties = properties;
	}

	private Session getSession() {
		if (session == null) {
			session = Session.getInstance(properties, new Authenticator() {
				/**
				 * Hack so that mail.password property can be used to supply a
				 * password.
				 */
				public PasswordAuthentication getPasswordAuthentication() {
					String password = properties.getProperty("mail."
							+ getRequestingProtocol() + ".password");
					if (password == null) {
						password = properties.getProperty("mail.password");
					}
					return new PasswordAuthentication(getDefaultUserName(),
							password);
				}
			});
		}
		return session;
	}

	private static Address[] toAddresses(Object v) throws TokenException {
		try {
			List<Address> out = new LinkedList<Address>();
			if (v instanceof Iterable) {
				for (Object x : (Iterable)v) {
					out.add(new InternetAddress(x.toString()));
				}
			} else {
				out.add(new InternetAddress(v.toString()));
			}
			return out.toArray(new Address[0]);
		} catch (AddressException e) {
			throw new JavaException(e);
		} catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(e.toString());
		}
	}

	/**
	 * Message being composed or viewed from a store.
	 * <ul>
	 * <li>setReplyTo: set reply-to address(es)
	 * <li>setText: set text (first plain-text part)
	 * <li>setSubject: set subject
	 * <li>text: get text (first plain-text part)
	 * <li>subject: get subject
	 * <li>delete: delete the message from the store
	 * <li>save: persist changes to the store
	 * <li>reply: create a new message in reply to the current one
	 * </ul>
	 * 
	 * @author quark
	 */
	public static class MailMessage extends DotSite {
		private Message message;

		public MailMessage(Message message) {
			this.message = message;
		}

		private static String findText(Object o) throws MessagingException,
				IOException {
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

		protected void addMethods() {
			addMethod("setReplyTo", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						message.setReplyTo(toAddresses(args.getArg(0)));
						return Value.signal();
					} catch (MessagingException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("setText", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						message.setText(args.stringArg(0));
						return Value.signal();
					} catch (MessagingException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("setSubject", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						message.setSubject(args.stringArg(0));
						return Value.signal();
					} catch (MessagingException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("reply", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						boolean replytoall = true;
						if (args.size() > 0) {
							replytoall = args.boolArg(0);
						}
						return new MailMessage(message.reply(replytoall));
					} catch (MessagingException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("subject", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return message.getSubject();
					} catch (MessagingException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("text", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						String text = findText(message.getContent());
						if (text == null) return "";
						else return text;
					} catch (MessagingException e) {
						throw new JavaException(e);
					} catch (IOException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("delete", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						message.setFlag(Flags.Flag.DELETED, true);
						return Value.signal();
					} catch (MessagingException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("save", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						message.saveChanges();
						return Value.signal();
					} catch (MessagingException e) {
						throw new JavaException(e);
					}
				}
			});
		}

		public String toString() {
			return super.toString() + "(" + message.toString() + ")";
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
	public static class MailFilter extends DotSite {
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

		private Object messages(Folder folder) throws TokenException {
			try {
				Message[] messages;
				if (term == null) {
					messages = folder.getMessages();
				} else {
					messages = folder.search(term);
				}
				Object[] messagesOut = new Object[messages.length];
				for (int i = 0; i < messages.length; ++i) {
					messagesOut[i] = new MailMessage(messages[i]);
				}
				return messagesOut;
			} catch (Exception e) {
				throw new JavaException(e);
			}
		}

		@Override
		protected void addMethods() {
			addMethod("and", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					SearchTerm otherTerm;
					try {
						otherTerm = ((MailFilter)args.getArg(0)).term;
					} catch (ClassCastException e) {
						throw new ArgumentTypeMismatchException(e.toString());
					}
					return filter(otherTerm);
				}
			});
			addMethod("or", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {

					SearchTerm otherTerm;
					try {
						otherTerm = ((MailFilter) args.getArg(0)).term;
					} catch (ClassCastException e) {
						throw new ArgumentTypeMismatchException(
								"Expected SearchTerm argument(s)");
					}
					return new MailFilter(term == null ? otherTerm
							: new OrTerm(term, otherTerm));
				}
			});
			addMethod("not", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return new MailFilter(new NotTerm(
							term == null ? new FlagTerm(new Flags(), false) // this
																			// should
																			// always
																			// match
									: term));
				}
			});
			addMethod("from", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return filter(new FromTerm(new InternetAddress(args
								.stringArg(0))));
					} catch (AddressException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("to", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return filter(new RecipientTerm(
								Message.RecipientType.TO, new InternetAddress(
										args.stringArg(0))));
					} catch (AddressException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("cc", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return filter(new RecipientTerm(
								Message.RecipientType.CC, new InternetAddress(
										args.stringArg(0))));
					} catch (AddressException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("bcc", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return filter(new RecipientTerm(
								Message.RecipientType.BCC, new InternetAddress(
										args.stringArg(0))));
					} catch (AddressException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("recipient", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						InternetAddress address = new InternetAddress(args
								.stringArg(0));
						return filter(new OrTerm(new SearchTerm[] {
								new RecipientTerm(Message.RecipientType.TO,
										address),
								new RecipientTerm(Message.RecipientType.CC,
										address),
								new RecipientTerm(Message.RecipientType.BCC,
										address) }));
					} catch (AddressException e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("body", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return filter(new BodyTerm(args.stringArg(0)));
				}
			});
			addMethod("subject", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return filter(new SubjectTerm(args.stringArg(0)));
				}
			});
			addMethod("header", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return filter(new HeaderTerm(args.stringArg(0), args
							.stringArg(1)));
				}
			});
		}
	}

	/**
	 * Folder containing messages or subfolders.
	 * 
	 * <ul>
	 * <li>open(): open the folder (use open("r") for read-only mode)
	 * <li>close(): close the folder
	 * <li>name(): name of the folder
	 * <li>fullName(): full path to the folder
	 * <li>messages(): return all messages
	 * <li>search(MailFilter): return filtered messages
	 * <li>list(): return a list of sub-folders
	 * <li>messageCount(): return total number of messages in folder (IGNORES
	 * FILTERS)
	 * <li>exists(): return true if this folder exists in the store
	 * <li>filter: construct a MailFilter
	 * </ul>
	 * 
	 * <p>
	 * FIXME: messageCount ignores filters.
	 * 
	 * @author quark
	 */
	public static class MailFolder extends DotSite {
		private Folder folder;

		public MailFolder(Folder folder) {
			this.folder = folder;
		}

		@Override
		protected void addMethods() {
			addMethod("open", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					int mode = Folder.READ_WRITE;
					if (args.size() > 0) {
						if (args.stringArg(0).equals("r")) {
							mode = Folder.READ_ONLY;
						}
					}
					try {
						folder.open(mode);
					} catch (Exception e) {
						throw new JavaException(e);
					}
					return Value.signal();
				}
			});
			addMethod("name", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return folder.getName();
					} catch (Exception e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("fullName", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return folder.getFullName();
					} catch (Exception e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("filter", new MailFilter());
			addMethod("messageCount", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return folder.getMessageCount();
					} catch (Exception e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("exists", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return folder.exists();
					} catch (Exception e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("list", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						Folder[] folders;
						if (args.size() > 0) {
							folders = folder.list(args.stringArg(0));
						} else {
							folders = folder.list();
						}
						Object[] foldersOut = new Object[folders.length];
						for (int i = 0; i < folders.length; ++i) {
							foldersOut[i] = new MailFolder(folders[i]);
						}
						return foldersOut;
					} catch (Exception e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("messages", new ThreadedSite() {
				public Object evaluate(Args args) throws TokenException {
					return new MailFilter().messages(folder);
				}
			});
			addMethod("search", new ThreadedSite() {
				public Object evaluate(Args args) throws TokenException {
					try {
						return ((MailFilter) args.getArg(0)).messages(folder);
					} catch (ClassCastException e) {
						throw new ArgumentTypeMismatchException(e.toString());
					}
				}
			});
			addMethod("close", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					boolean expunge = true;
					if (args.size() > 0) {
						expunge = args.boolArg(0);
					}
					try {
						folder.close(expunge);
					} catch (Exception e) {
						throw new JavaException(e);
					}
					return Value.signal();
				}
			});
		}

		public String toString() {
			return super.toString() + "(" + folder.toString() + ")";
		}
	}

	/**
	 * Mailbox.
	 * 
	 * <ul>
	 * <li>connect(?username, ?password): connect to the mailbox
	 * <li>close(): close the connection
	 * <li>folder(name): access a MailFolder
	 * <li>defaultFolder(): access the default (root) folder
	 * </ul>
	 * 
	 * @author quark
	 */
	public static class MailStore extends DotSite {
		private Store store;

		public MailStore(Store store) {
			this.store = store;
		}

		@Override
		protected void addMethods() {
			addMethod("connect", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						if (args.size() == 0) {
							store.connect();
						} else {
							store.connect(args.stringArg(0), args.stringArg(1));
						}
					} catch (Exception e) {
						throw new JavaException(e);
					}
					return Value.signal();
				}
			});
			addMethod("close", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						store.close();
					} catch (Exception e) {
						throw new JavaException(e);
					}
					return Value.signal();
				}
			});
			addMethod("folder", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						return new MailFolder(store
								.getFolder(args.stringArg(0)));
					} catch (Exception e) {
						throw new JavaException(e);
					}
				}
			});
			addMethod("defaultFolder", new ThreadedSite() {
				@Override
				public Value evaluate(Args args) throws TokenException {
					try {
						return new MailFolder(store.getDefaultFolder());
					} catch (Exception e) {
						throw new JavaException(e);
					}
				}
			});
		}

		public String toString() {
			return super.toString() + "(" + store.toString() + ")";
		}
	}

	/**
	 * Outgoing transport (SMTP).
	 * 
	 * <ul>
	 * <li>connect(?username, ?password): connect to the server
	 * <li>close(): close the connection
	 * <li>send(MailMessage): send a message
	 * </ul>
	 * 
	 * @author quark
	 */
	public static class MailTransport extends DotSite {
		private Transport transport;

		public MailTransport(Transport transport) {
			this.transport = transport;
		}

		@Override
		protected void addMethods() {
			addMethod("connect", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						if (args.size() == 0) {
							transport.connect();
						} else {
							transport.connect(null, args.stringArg(0), args
									.stringArg(1));
						}
					} catch (Exception e) {
						throw new JavaException(e);
					}
					return Value.signal();
				}
			});
			addMethod("send", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						Message m = ((MailMessage) args.getArg(0)).message;
						m.saveChanges();
						transport.sendMessage(m, m.getAllRecipients());
					} catch (ClassCastException e) {
						throw new ArgumentTypeMismatchException(e.toString());
					} catch (Exception e) {
						throw new JavaException(e);
					}
					return Value.signal();
				}
			});
			addMethod("close", new ThreadedSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					try {
						transport.close();
					} catch (Exception e) {
						throw new JavaException(e);
					}
					return Value.signal();
				}
			});
		}
	}

	@Override
	protected void addMethods() {
		addMethod("property", new PartialSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				return properties.get(args.stringArg(0));
			}
		});
		addMethod("withProperties", new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				Properties newProperties = (Properties) properties.clone();
				Iterable properties;
				try {
					if (args.size() == 1) {
						properties = (Iterable)args.getArg(0);
					} else {
						properties = Arrays.asList(args.asArray());
					}
					for (Iterator it = properties.iterator(); it.hasNext();) {
						newProperties.setProperty((String)it.next(), (String)it.next());
					}
				} catch (ClassCastException e) {
					throw new JavaException(e);
				}
				return new Mail(newProperties);
			}
		});
		addMethod("transport", new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				try {
					if (args.size() == 0) {
						return new MailTransport(getSession().getTransport());
					} else {
						URLName url = new URLName(args.stringArg(0));
						return new MailTransport(getSession().getTransport(url));
					}
				} catch (Exception e) {
					throw new JavaException(e);
				}
			}
		});
		addMethod("store", new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				try {
					if (args.size() == 0) {
						return new MailStore(getSession().getStore());
					} else {
						URLName url = new URLName(args.stringArg(0));
						return new MailStore(getSession().getStore(url));
					}
				} catch (Exception e) {
					throw new JavaException(e);
				}
			}
		});
		/** subject, body, to, from */
		addMethod("message", new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				try {
					MimeMessage m = new MimeMessage(getSession());
					if (args.size() > 0) {
						m.setSubject(args.stringArg(0));
					}
					if (args.size() > 1) {
						m.setText(args.stringArg(1));
					}
					if (args.size() > 2) {
						m.setRecipients(Message.RecipientType.TO,
								toAddresses(args.getArg(2)));
					}
					if (args.size() > 3) {
						m.setFrom(new InternetAddress(args.stringArg(3)));
					}
					return new MailMessage(m);
				} catch (TokenException e) {
					throw e;
				} catch (ClassCastException e) {
					throw new ArgumentTypeMismatchException(e.toString());
				} catch (Exception e) {
					throw new JavaException(e);
				}
			}
		});
	}
}

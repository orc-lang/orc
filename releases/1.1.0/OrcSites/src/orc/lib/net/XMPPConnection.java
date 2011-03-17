//
// XMPPConnection.java -- Java class XMPPConnection
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.util.LinkedList;

import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.values.Value;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * Orc support for the XMPP (Jabber, Google Talk) messaging protocol.
 * 
 * <p>For the most part, the API follows that of <a href="http://www.igniterealtime.org/projects/smack/">Smack</a>.
 * This example program should get you started:
 * 
 * <pre>
 * var (user, pass) = ("USER", "PASS")
 * var talkto = "USER@gmail.com"
 * site XMPPConnection = orc.lib.net.XMPPConnection
 * val conn = XMPPConnection("talk.google.com", 5222, "gmail.com") >conn>
 *   conn.connect() >>
 *   conn.login(user, pass) >>
 *   conn
 * conn.chat(talkto) >chat>
 * chat.send("Are you there?") >>
 * chat.receive()
 * </pre>
 * 
 * <p>WARNING: to talk to someone you must appear in their buddy list.
 * Currently you will have to handle this manually before trying to talk to them
 * using Orc.
 * 
 * <p>
 * I wish I could just use our Java sites to generate this automatically, but
 * there are a few methods in the API which require asynchronous behavior and so
 * must be implemented as Orc sites.
 * 
 * @author quark
 */
public class XMPPConnection extends EvalSite {
	/**
	 * XMPP connection/session. Members include connect, disconnect, login, and
	 * chat. The underlying methods are all synchronous and therefore most of
	 * the site methods are threaded.
	 * 
	 * <p>
	 * For details on the methods, refer to the Smack javadoc.
	 */
	private static class XMPPConnectionSite extends DotSite {
		private final org.jivesoftware.smack.XMPPConnection connection;

		public XMPPConnectionSite(final ConnectionConfiguration config) {
			connection = new org.jivesoftware.smack.XMPPConnection(config);
		}

		@Override
		protected void addMembers() {
			addMember("connect", new ThreadedSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					try {
						connection.connect();
					} catch (final XMPPException e) {
						throw new SiteException("XMPP connection error: " + e.getMessage(), e);
					}
					return Value.signal();
				}
			});
			addMember("disconnect", new ThreadedSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					connection.disconnect();
					return Value.signal();
				}
			});
			addMember("login", new ThreadedSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					try {
						switch (args.size()) {
						case 4:
							connection.login(args.stringArg(0), args.stringArg(1), args.stringArg(2), args.boolArg(3));
							break;
						case 3:
							connection.login(args.stringArg(0), args.stringArg(1), args.stringArg(2));
							break;
						default:
							connection.login(args.stringArg(0), args.stringArg(1));
							break;
						}
					} catch (final XMPPException e) {
						throw new SiteException("XMPP login error: " + e.getMessage(), e);
					}
					return Value.signal();
				}
			});
			addMember("chat", new ThreadedSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					return new ChatSite(connection, args.stringArg(0));
				}
			});
		}

		@Override
		public void finalize() {
			connection.disconnect();
		}
	}

	/**
	 * Ongoing chat with a user. Members include send and receive, which
	 * currently support only simple string messages.
	 * 
	 * @author quark
	 */
	private static class ChatSite extends DotSite implements MessageListener {
		private final Chat chat;
		/** Buffer for received messages. */
		private final LinkedList<Object> received = new LinkedList<Object>();
		/** Queue of tokens waiting to receive messages. */
		private final LinkedList<Token> receivers = new LinkedList<Token>();

		public ChatSite(final org.jivesoftware.smack.XMPPConnection connection, final String account) {
			this.chat = connection.getChatManager().createChat(account, this);
		}

		/**
		 * Asynchronous listener for received messages. The messages are sent to
		 * tokens waiting on the received site if any, otherwise they are buffed.
		 */
		public void processMessage(final Chat _, final Message message) {
			synchronized (received) {
				final Object v = message.getBody();
				if (receivers.isEmpty()) {
					received.add(v);
				} else {
					final Token receiver = receivers.removeFirst();
					receiver.resume(v);
				}
			}
		}

		@Override
		protected void addMembers() {
			/**
			 * Send a message.
			 */
			addMember("send", new ThreadedSite() {
				@Override
				public Object evaluate(final Args args) throws TokenException {
					try {
						chat.sendMessage(args.stringArg(0));
					} catch (final XMPPException e) {
						throw new SiteException("XMPP message send error: " + e.getMessage(), e);
					}
					return Value.signal();
				}
			});
			/**
			 * Receive a message (blocks until one is received).
			 */
			addMember("receive", new Site() {
				@Override
				public void callSite(final Args args, final Token receiver) {
					synchronized (received) {
						if (received.isEmpty()) {
							receivers.addLast(receiver);
						} else {
							receiver.resume(received.removeFirst());
						}
					}
				}
			});
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		ConnectionConfiguration config;
		if (args.size() > 2) {
			config = new ConnectionConfiguration(args.stringArg(0), args.intArg(1), args.stringArg(2));
		} else if (args.size() > 1) {
			config = new ConnectionConfiguration(args.stringArg(0), args.intArg(1));
		} else {
			config = new ConnectionConfiguration(args.stringArg(0));
		}
		return new XMPPConnectionSite(config);
	}
}
//
// XMPPConnection.java -- Java class XMPPConnection
// Project OrcSites
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.util.LinkedList;

import orc.CallContext;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * Orc support for the XMPP (Jabber, Google Talk) messaging protocol.
 * <p>
 * For the most part, the API follows that of <a
 * href="http://www.igniterealtime.org/projects/smack/">Smack</a>. This example
 * program should get you started:
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
 * <p>
 * WARNING: to talk to someone you must appear in their buddy list. Currently
 * you will have to handle this manually before trying to talk to them using
 * Orc.
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
     * <p>
     * For details on the methods, refer to the Smack javadoc.
     */
    private static class XMPPConnectionSite extends DotSite {
        protected final org.jivesoftware.smack.XMPPConnection connection;

        public XMPPConnectionSite(final ConnectionConfiguration config) {
            connection = new org.jivesoftware.smack.XMPPConnection(config);
        }

        @Override
        protected void addMembers() {
            addMember("connect", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    try {
                        connection.connect();
                    } catch (final XMPPException e) {
                        throw new JavaException(e);
                    }
                    return signal();
                }
            });
            addMember("disconnect", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    connection.disconnect();
                    return signal();
                }
            });
            addMember("login", new EvalSite() {
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
                        throw new JavaException(e);
                    }
                    return signal();
                }
            });
            addMember("chat", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    return new ChatSite(XMPPConnectionSite.this, args.stringArg(0));
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
        private final LinkedList<CallContext> receivers = new LinkedList<CallContext>();
        private final XMPPConnectionSite xmppConnectionSite;

        public ChatSite(final XMPPConnectionSite xmppConnectionSite, final String account) {
            this.xmppConnectionSite = xmppConnectionSite;
            this.chat = this.xmppConnectionSite.connection.getChatManager().createChat(account, this);
        }

        /**
         * Asynchronous listener for received messages. The messages are sent to
         * tokens waiting on the received site if any, otherwise they are
         * buffed.
         */
        @Override
        public void processMessage(final Chat chat, final Message message) {
            // System.out.println(getClass().getSimpleName()+" processMessage ihc="+System.identityHashCode(this));
            synchronized (received) {
                final Object v = message.getBody();
                if (receivers.isEmpty()) {
                    received.add(v);
                } else {
                    final CallContext receiver = receivers.removeFirst();
                    receiver.publish(v);
                }
            }
        }

        @Override
        protected void addMembers() {
            /**
             * Send a message.
             */
            addMember("send", new EvalSite() {
                @Override
                public Object evaluate(final Args args) throws TokenException {
                    // System.out.println(getClass().getSimpleName()+" send ihc="+System.identityHashCode(this));
                    try {
                        chat.sendMessage(args.stringArg(0));
                    } catch (final XMPPException e) {
                        throw new JavaException(e);
                    }
                    return signal();
                }
            });
            /**
             * Receive a message (blocks until one is received).
             */
            addMember("receive", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext receiver) {
                    // System.out.println(getClass().getSimpleName()+" receive ihc="+System.identityHashCode(this));
                    synchronized (received) {
                        if (received.isEmpty()) {
                            // System.out.println("(enqueued)");
                            receivers.addLast(receiver);
                        } else {
                            // System.out.println("(immed publish)");
                            receiver.publish(received.removeFirst());
                        }
                    }
                }
            });
        }
    }

    @Override
    public Object evaluate(final Args args) throws TokenException {
        // System.out.println(getClass().getSimpleName()+" () ihc="+System.identityHashCode(this));
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

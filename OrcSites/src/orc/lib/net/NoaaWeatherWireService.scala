//
// NoaaWeatherWireService.scala -- Scala classes NoaaWeatherWireService, NwwsReceiver, and NwwsOiExtensionElement
// Project OrcSites
//
// Created by jthywiss on Feb 21, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net

import java.io.StringWriter
import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }
import java.util.concurrent.atomic.AtomicBoolean

import scala.util.control.NonFatal

import orc.CallContext
import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException, JavaException }
import orc.types.{ FunctionType, StringType, Type }
import orc.util.ArrayExtensions.Array2
import orc.values.sites.{ Site, SpecificArity, TalkativeSite, TypedSite }

import javax.net.ssl.SSLSocketFactory
import org.jivesoftware.smack.{ SmackConfiguration, StanzaListener }
import org.jivesoftware.smack.filter.FromMatchesFilter
import org.jivesoftware.smack.packet.{ ExtensionElement, Message, Stanza }
import org.jivesoftware.smack.provider.{ ExtensionElementProvider, ProviderManager }
import org.jivesoftware.smack.tcp.{ XMPPTCPConnection, XMPPTCPConnectionConfiguration }
import org.jivesoftware.smackx.caps.EntityCapsManager
import org.jivesoftware.smackx.caps.packet.CapsExtension
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.xmlpull.mxp1_serializer.MXSerializer
import org.xmlpull.v1.{ XmlPullParser, XmlSerializer }

/** Stream weather bulletins from the US National Weather Service.  The
  * stream includes all textual products from US weather offices and centers.
  *
  * The messages are transmitted by the NOAA Weather Wire Service Open
  * Interface (NWWS-OI). NWWS-OI is implemented as a XMPP multi-user chat
  * room. A user ID and password issued by the NWS is required.  See URL:
  * http://www.nws.noaa.gov/nwws/
  *
  * @author jthywiss
  */
class NoaaWeatherWireService extends Site with SpecificArity with TypedSite with TalkativeSite {
  val arity = 2

  override def call(args: Array[AnyRef], callContext: CallContext): Unit = {
    args match {
      case Array2(userId: String, password: String) => streamMessages(userId, password, callContext)
      case Array2(_: String, a) => throw new ArgumentTypeMismatchException(1, "String", if (a != null) a.getClass().toString() else "null")
      case Array2(a, _) => throw new ArgumentTypeMismatchException(0, "String", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }

  override def orcType(): Type = FunctionType(Nil, List(StringType, StringType), StringType)

  protected def streamMessages(userId: String, password: String, callContext: CallContext): Unit = {
    try {
      val r = new NwwsReceiver(userId, password)
      r.run(processMessage(callContext.materialize())(_))
    } catch {
      case ie: InterruptedException => { callContext.halt; throw ie }
      case e: Exception if NonFatal(e) => { callContext.halt(new JavaException(e)) }
    }
  }

  protected def processMessage(callContext: CallContext)(message: NwwsOiExtensionElement): Unit = {
    //TODO: Publish a record with the fields of NwwsOiExtensionElement
    val bulletin = message.content
    callContext.publishNonterminal(bulletin)
  }
}

/** Receive messages from the NOAA Weather Wire Service Open Interface (NWWS-OI).
  * NWWS-OI is implemented as a XMPP multi-user chat room.
  *
  * @author jthywiss
  */
class NwwsReceiver(nwwsUserId: String, nwwsPassword: String, connectResourceName: String = "Orc", mucNickanme: String = "UTexas-CS-Orc-" + java.util.UUID.randomUUID.toString, filter: NwwsOiExtensionElement => Boolean = { _ => true }) extends StanzaListener {
  val nwwsXmppServiceName = "nwws-oi.weather.gov"
  val nwwsXmppHostName = "nwws-oi.weather.gov"
  val nwwsXmppPort = 5223
  val nwwsMucRoomId = "nwws@conference.nwws-oi.weather.gov"
  val nwwsSenderJid = nwwsMucRoomId + "/nwws-oi"

  val shutdown = new AtomicBoolean()
  val receivedMessages: BlockingQueue[NwwsOiExtensionElement] = new LinkedBlockingQueue[NwwsOiExtensionElement]()

  def run(consumeMessage: NwwsOiExtensionElement => Unit): Unit = {
    Logger.finer("Configuring XMPP client (Smack)...")
    val nwwsConnection = configureConnection()

    try {
      Logger.finer(s"Connecting to NWWS XMPP Server ${nwwsXmppHostName}:${nwwsXmppPort}...")
      connect(nwwsConnection)
      Logger.finer(s"Logging in as ${nwwsUserId}@${nwwsXmppServiceName}/${connectResourceName}...")
      login(nwwsConnection)

      nwwsConnection.addAsyncStanzaListener(this, new FromMatchesFilter(nwwsSenderJid, false))

      val nwwsMucRoom = getMucRoom(nwwsConnection)

      try {
        Logger.finer(s"Joining MUC room ${nwwsMucRoomId} as ${mucNickanme}...")
        nwwsMucRoom.join(mucNickanme)
        Logger.finer("Awaiting and processing messages...")
        awaitAndProcessMessages(consumeMessage)
      } finally {
        shutdown.set(true)
        Logger.finer("Leaving MUC...")
        nwwsMucRoom.leave()
      }
    } finally {
      shutdown.set(true)
      Logger.finer("Disconnecting from NWWS XMPP Server...")
      nwwsConnection.disconnect()
    }
    Logger.finer("Done, exiting")
  }

  def stop(): Unit = shutdown.set(true)

  protected def configureConnection() = {
    SmackConfiguration.setDefaultPacketReplyTimeout(10000)
    /* NWWS sends legacy caps (missing hash attribute) and Smack throws warnings for each one, so disable caps altogether. */
    ProviderManager.removeExtensionProvider(CapsExtension.ELEMENT, CapsExtension.NAMESPACE)
    ProviderManager.addExtensionProvider(NwwsOiExtensionElement.getElementName, NwwsOiExtensionElement.getNamespace, NwwsOiExtensionElement);
    val config = XMPPTCPConnectionConfiguration.builder()
      .setUsernameAndPassword(nwwsUserId, nwwsPassword)
      .setResource(connectResourceName)
      .setServiceName(nwwsXmppServiceName)
      .setHost(nwwsXmppHostName)
      .setPort(nwwsXmppPort)
      .setSocketFactory(SSLSocketFactory.getDefault())
      .setSendPresence(false)
      .setDebuggerEnabled(true)
      .build()
    val connection = new XMPPTCPConnection(config)
    EntityCapsManager.getInstanceFor(connection).disableEntityCaps()
    connection
  }

  protected def connect(nwwsConnection: XMPPTCPConnection) = {
    nwwsConnection.connect()
  }

  protected def login(nwwsConnection: XMPPTCPConnection) = {
    nwwsConnection.login()
  }

  protected def getMucRoom(nwwsConnection: XMPPTCPConnection) = {
    val manager: MultiUserChatManager = MultiUserChatManager.getInstanceFor(nwwsConnection)
    manager.getMultiUserChat(nwwsMucRoomId)
  }

  override def processPacket(packet: Stanza): Unit = {
    packet match {
      case m: Message => {
        m.getExtension("nwws-oi") match {
          case b: NwwsOiExtensionElement if filter(b) => receivedMessages.put(b)
          case _ => /* Discard non-NwwsOiExtensionElement extensions */
        }
      }
      case _ => /* Discard non-Message Stanzas */
    }
  }

  protected def awaitAndProcessMessages(consumeMessage: NwwsOiExtensionElement => Unit) = {
    while (!shutdown.get) {
      consumeMessage(receivedMessages.take())
    }
  }

}

object NwwsOiExtensionElement extends ExtensionElementProvider[NwwsOiExtensionElement] {
  def getNamespace = "nwws-oi"
  def getElementName = "x"
  override def parse(parser: XmlPullParser, initialDepth: Int): NwwsOiExtensionElement = {
    if (parser.getText() != null) {
      val dataDesignator = parser.getAttributeValue(null, "ttaaii")
      val originLocation = parser.getAttributeValue(null, "cccc")
      val originationTime = parser.getAttributeValue(null, "issue")
      val awipsId = parser.getAttributeValue(null, "awipsid")
      val id = parser.getAttributeValue(null, "id")
      /* NWWS is trashing the CR-CR-LF into LF-LF and dropping the SOH and
       * ETX characters, so we normalize the text into LF-terminated lines. */
      val content = parser.nextText().replaceAllLiterally("\n\n", "\n").stripPrefix("\n")
      NwwsOiExtensionElement(dataDesignator, originLocation, originationTime, awipsId, id, content)
    } else {
      null
    }
  }
}

case class NwwsOiExtensionElement(dataDesignator: String, originLocation: String, originationTime: String, awipsId: String, id: String, content: String) extends ExtensionElement {
  override def getNamespace = NwwsOiExtensionElement.getNamespace
  override def getElementName = NwwsOiExtensionElement.getElementName
  override def toXML(): CharSequence = {
    val xmlSerializer: XmlSerializer = new MXSerializer()
    val stringWriter = new StringWriter()
    xmlSerializer.setOutput(stringWriter)

    /* XMPP doesn't usually use XML namespace prefixes */
    xmlSerializer.startTag(null, getElementName)
    xmlSerializer.attribute(null, "xmlns", getNamespace)
    if (originLocation != null) xmlSerializer.attribute(null, "cccc", originLocation)
    if (dataDesignator != null) xmlSerializer.attribute(null, "ttaaii", dataDesignator)
    if (originationTime != null) xmlSerializer.attribute(null, "issue", originationTime)
    if (awipsId != null) xmlSerializer.attribute(null, "awipsid", awipsId)
    if (id != null) xmlSerializer.attribute(null, "id", id)
    xmlSerializer.cdsect(content)
    xmlSerializer.endTag(null, getElementName)

    stringWriter.toString
  }
}

{- xmpp.orc -- Orc program demonstrating ELIZA over XMPP (Google Talk)

Send "goodbye", "bye", "exit", or "quit" to end the conversation.
 -}

include "fun.inc"
include "net.inc"

val USERNAME = "you@gmail.com"
val PASSWORD = "your password"
val FRIEND = "your friend@gmail.com"

val conn = XMPPConnection("talk.google.com", 5222, "gmail.com")

val chat =
  conn.connect() >>
  conn.login(USERNAME, PASSWORD) >>
  conn.chat(FRIEND)

def ElizaChat(init) =
  val eliza = Eliza()
  def loop(message, false) =
    eliza(chat.send(message) >> chat.receive()) >response>
    loop(response, eliza.finished())
  def loop(message, true) =
    chat.send(message) >> conn.disconnect()
  loop(init, false)

ElizaChat("How do you do.  Please tell me your problem.") >>
"Chat finished"

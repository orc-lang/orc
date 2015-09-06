{-
Requests your Google Talk account information and the username of a friend,
logs into Google Talk and starts a conversation with your friend.

Send "goodbye", "bye", "exit", or "quit" to end the conversation.
-}

include "fun.inc"
include "net.inc"
include "forms.inc"

val (USERNAME, PASSWORD, WHO) =
  WebPrompt("Chat Information", [
    Mandatory(Textbox("username", "Google Talk Username")),
    Mandatory(PasswordField("password", "Google Talk Password")),
    Mandatory(Textbox("who", "Friend's Username")),
    Button("submit", "Go") ]) >data>
  (data.get("username"), data.get("password"), data.get("who"))

val conn = XMPPConnection("talk.google.com", 5222, "gmail.com")

val chat =
  conn.connect() >>
  conn.login(USERNAME, PASSWORD) >>
  conn.chat(WHO)

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

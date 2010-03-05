{-
Requests your Google Talk account information and the username of a friend,
logs into Google Talk and starts a conversation with your friend.
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

val chat =
  val c = XMPPConnection("talk.google.com", 5222, "gmail.com")
  c.connect() >>
  c.login(USERNAME, PASSWORD) >>
  c.chat(WHO)

def ElizaChat(init) =
  val eliza = Eliza()
  def loop(message) =
    eliza(chat.send(message) >> chat.receive()) >response>
    loop(response)
  loop(init)

ElizaChat("How do you do.  Please tell me your problem.")

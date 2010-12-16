include "fun.inc"
include "net.inc"

val USERNAME = "you@gmail.com"
val PASSWORD = "your password"
val FRIEND = "your friend@gmail.com"

val chat =
	val c = XMPPConnection("talk.google.com", 5222, "gmail.com")
	c.connect() >>
	c.login(USERNAME, PASSWORD) >>
	c.chat(FRIEND)

def ElizaChat(init) =
    val eliza = Eliza()
    def loop(message) =
        eliza(chat.send(message) >> chat.receive()) >response>
        loop(response)
    loop(init)
	
ElizaChat("How do you do.  Please tell me your problem.")

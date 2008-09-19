include "fun.inc"
include "net.inc"

def ElizaChat(chat, init) =
    val eliza = Eliza()
    def loop(message) =
        eliza(chat.send(message) >> chat.receive()) >response>
        loop(response)
    loop(init)

val chat =
	val c = XMPPConnection("talk.google.com", 5222, "gmail.com")
	c.connect() >>
	c.login("orchardserver@gmail.com", "ckyogack") >>
	c.chat("adrianquark@gmail.com")
	
ElizaChat(chat, "How do you do.  Please tell me your problem.")
include "fun.inc"
include "net.inc"

val USERNAME = "orchardserver@gmail.com"
val PASSWORD = "ckyogack"
val FRIEND = "jthywissen@gmail.com"

val chat =
	val c = XMPPConnection("talk.google.com", 5222, "gmail.com")
	println("connecting") >> c.connect() >>
	println("logging in") >> c.login(USERNAME, PASSWORD) >>
	println("chatting") >> c.chat(FRIEND)

def ElizaChat(init) =
    val eliza = Eliza()
    def loop(message) =
        print(message) >> eliza(chat.send(message) >> println(" -- sent") >> chat.receive() >r> print("rcv: "+r) >> r) >response>
        loop(response)
    loop(init)
	
ElizaChat("How do you do.  Please tell me your problem.") | Rtimer(1000)
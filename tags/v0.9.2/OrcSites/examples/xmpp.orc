site XMPPConnection = orc.lib.xmpp.XMPPConnection

def echo(chat) = lambda () = chat.receive() >x> chat.send(x)

chat.send("Hello!") >> null | repeat(echo(chat)) >> null
	<chat<
		c.connect() >>
		c.login("USER", "PASS") >>
		c.chat("USER@gmail.com")
	<c< XMPPConnection("talk.google.com", 5222, "gmail.com")
{- mail.orc -- Orc program that mails "Hello" to the given address
 -}

include "mail.inc"

val mailer = MailerFactory("mail.properties")
val TO = Prompt("To:")

val message = mailer.newMessage("Hello", "Hello from Orc!", TO)
val transport = mailer.getTransport()
transport.connect() >>
transport.send(message) >>
transport.close()

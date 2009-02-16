include "mail.inc"

val mailer = MailerFactory("mail.properties")
val TO = "test@example.com"

val message = mailer.newMessage("Hello", "Hello", TO)
val transport = mailer.getTransport()
transport.connect() >>
transport.send(message) >>
transport.close()
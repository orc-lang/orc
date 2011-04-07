import site MailerFactory = "orc.lib.net.MailerFactory"
import site MailListener = "orc.lib.orchard.MailListenerFactory"

{-
Simplified mail sending interface.
-}
def SendMail(to, subject, body) =
  val mailer = MailerFactory("orc/orchard/orchard.properties")
  val message = mailer.newMessage(subject, body, to)
  val outbox = mailer.getTransport()
  outbox.connect() >>
  outbox.send(message) >>
  outbox.close()
  
{-
Simplified mail sending interface.
-}
def SendMailFrom(from, to, subject, body) =
  val mailer = MailerFactory("orc/orchard/orchard.properties")
  val message =
    mailer.newMessage(subject, body, to) >m>
    m.setFrom(mailer.toAddress(from)) >>
    m
  val outbox = mailer.getTransport()
  outbox.connect() >>
  outbox.send(message) >>
  outbox.close()
  
{-
Send an email to the given address(es).
Return a site which can be used to poll for responses.
Each time the returned site is called, it will return the 
body of the next response.
-}
def MailPrompt(to, subject, body) =
  val mailer = MailerFactory("orc/orchard/orchard.properties")
  val listener = MailListener(mailer)
  SendMailFrom(listener.getAddress(), to, subject, body) >>
  listener.get() >m>
  m.getText() >text>
  listener.close() >>
  text

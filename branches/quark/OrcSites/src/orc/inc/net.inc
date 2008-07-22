site localhost = orc.lib.net.Localhost
site Webservice = orc.lib.net.Webservice
site Ping = orc.lib.net.Ping
site Google = orc.lib.net.GoogleSearch

site Mail = orc.lib.net.Mail

{-
Before using this, you should define the properties:
mail.from, mail.user, mail.password, mail.from.user
-}
val GMail = Mail.withProperties(
	"mail.smtps.auth", "true",
	"mail.store.protocol", "pop3s",
	"mail.transport.protocol", "smtps",
	"mail.smtps.host", "smtp.gmail.com",
	"mail.pop3s.host", "pop.gmail.com",
	"mail.from.host", "gmail.com",
	"mail.from.separator", "+",
	"mail.inbox", "INBOX")

{-
Simplified mail sending interface.
-}
def SendMail(from, to, subject, body, smtp_server) =
	val message = Mail.message(subject, body, to, from)
	val transport = Mail.transport(cat("smtp://", smtp_server))
	transport.connect() >>
	transport.send(message) >>
	transport.close()

{-
Check a folder for messages at regular intervals.
This method opens and closes the folder during each check.
Return a list of messages when they become available
(leaving the folder open so you can get the message content).
FIXME: if this is terminated it can leave an open connection.
-}
def PollMail(folder, filter, interval) =
	println("CHECKING") >>
	folder.open() >>
	folder.search(filter) >ms> (
		ms >[]> folder.close() >> Rtimer(interval) >> PollMail(folder, filter, interval)
		| ms >_:_> ms
	)

{-
Send an email to the given address(es).
Return a site which can be used to poll for responses.
Each time the returned site is called, it will return the 
body of the next response.

Requires the following custom properties to be defined
on the mail object:
mail.from.user, mail.from.separator, mail.from.host,
mail.inbox

FIXME: if this is terminated it can leave an open connection.
-}
def MailQuery(mail, to, subject, body) =
	val from = cat(
		mail.property("mail.from.user"),
		mail.property("mail.from.separator"),
		UUID(),
		"@",
		mail.property("mail.from.host"))
	val inbox = mail.store() >x>
		x.connect() >>
		x.folder(mail.property("mail.inbox"))
	val outbox = mail.transport() >x>
		x.connect() >>
		x
	val message = mail.message(subject, body, to, from) >m>
		m.setReplyTo(from) >>
		m
	outbox.send(message) >>
	outbox.close() >>
	lambda () =
		PollMail(inbox, inbox.filter.to(from), 10000) >m:_>
		m.text() >text>
		m.delete() >>
		inbox.close() >>
		text
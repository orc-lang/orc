val GMail = Mail.with(
	"mail.smtps.auth", "true",
	"mail.store.protocol", "pop3s",
	"mail.transport.protocol", "smtps",
	"mail.smtps.host", "smtp.gmail.com",
	"mail.pop3s.host", "pop.gmail.com",
	"mail.from", "orchardserver@gmail.com",
	"mail.user", "orchardserver@gmail.com",
	"mail.password", "ckyogack")

{-
Check a folder for messages at regular intervals.
This method opens and closes the folder during each check.
Return a list of messages when they become available
(leaving the folder open so you can get the message content).
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
-}
def MailQuery(to, subject, body) =
	val from = cat("orchardserver","+",UUID(),"@gmail.com")
	val inbox = GMail.store() >x>
		x.connect() >>
		x.folder("INBOX")
	val outbox = GMail.transport() >x>
		x.connect() >>
		x
	val message = GMail.message(subject, body, to, from) >m>
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

MailQuery("adrian@sixfingeredman.net",
	"hi from orc", "please respond") >r>
r()
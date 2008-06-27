val GMail = Mail.with(
	"mail.smtps.auth", "true",
	"mail.store.protocol", "pop3s",
	"mail.transport.protocol", "smtps",
	"mail.smtps.host", "smtp.gmail.com",
	"mail.pop3s.host", "pop.gmail.com",
	"mail.user", "orchardserver@gmail.com",
	"mail.password", "ckyogack")

def MailQuery(to, subject, body) =
	val from = cat("orchardserver","+",UUID(),"@gmail.com")
	val inbox = GMail.store() >x>
		x.connect() >>
		x.folder("INBOX")
	val outbox = GMail.transport() >x>
		x.connect() >>
		x
	def poll(i) =
		println("CHECKING") >>
		inbox.open() >>
		inbox.filter.to(from).messages() >ms> (
			ms >[]> inbox.close() >> Rtimer(i) >> poll(i)
			| ms >m:_> m
		)
	def receive() =
		poll(10000) >m>
		println("RECEIVED") >>
		m.text() >text>
		m.delete() >>
		inbox.close() >>
		text
	val message = GMail.message(subject, body, to, from) >m>
		m.setReplyTo(from) >>
		m
	outbox.send(message) >> (
		outbox.close() >> null
		| receive
	)

MailQuery("adrian@sixfingeredman.net",
	"hi from orc", "please respond") >r>
r()
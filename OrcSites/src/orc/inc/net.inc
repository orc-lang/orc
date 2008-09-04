site localhost = orc.lib.net.Localhost
site Webservice = orc.lib.net.Webservice
site Ping = orc.lib.net.Ping
site GoogleSearchFactory = orc.lib.net.GoogleSearchFactory
site YahooSearchFactory = orc.lib.net.YahooSearchFactory
site XMPPConnection = orc.lib.net.XMPPConnection

site MailerFactory = orc.lib.net.MailerFactory

{-
Simplified mail sending interface.
-}
def SendMail(to, subject, body) =
	val mailer = MailerFactory("mail.properties")
	val message = mailer.newMessage(subject, body, to)
	val transport = mailer.getTransport()
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
	folder.open() >>
	filter.search(folder) >ms> (
		ms >[]> folder.close() >> Rtimer(interval) >> PollMail(folder, filter, interval)
		| ms >_:_> ms
	)

{-
Send an email to the given address(es).
Return a site which can be used to poll for responses.
Each time the returned site is called, it will return the 
body of the next response.

FIXME: if this is terminated it can leave an open connection.
-}
def MailQuery(mailer, to, subject, body) =
	val from = mailer.newFromAddress()
	val inbox = mailer.getStore() >x>
		x.connect() >>
		x.getInbox()
	val outbox = mailer.getTransport() >x>
		x.connect() >>
		x
	val message = mailer.newMessage(subject, body, to) >m>
		m.setReplyTo(mailer.toAddresses(from)) >>
		m
	val filter = mailer.newFilter() >f>
		f.to(from.toString())
	def PollAndDelete() =
		PollMail(inbox, filter, 5000) >m:_>
		m.getText() >text>
		m.delete() >>
		inbox.close() >>
		text
	outbox.send(message) >>
	outbox.close() >>
	PollAndDelete
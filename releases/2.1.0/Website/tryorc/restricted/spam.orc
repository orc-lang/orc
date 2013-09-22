{-
Prompts the user for:
- subject
- from address
- list of email addresses (in a text file)
- email body (in a text file)
and sends a mass email.
-}
include "mail.inc"
include "forms.inc"

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

-- returns (name, address)
def parseRecipients(text) =
  def parseRecipient(line) =
    val line = line.trim()
    val space = line.lastIndexOf(" ")
    val tab = line.lastIndexOf("\t")
    val splitAt = if space <: tab then tab else space
    if splitAt :> 0 then
    (line.substring(0, splitAt).trim(),
     line.substring(splitAt+1))
    else ("", line)
  map(parseRecipient, lines(text))

-- returns: (from, subject, body, [(name,address)])
def getData() =
WebPrompt("Upload Data", [
    Mandatory(Textbox("from", "From")),
    Mandatory(Textbox("subject", "Subject")),
    FormInstructions("recipientsi",
      "Recipients should be one per line: name and email address, separated by space."),
  Mandatory(UploadField("recipients", "Recipients")),
    FormInstructions("bodyi",
     "Any appearance of {{NAME}} in the email body will be replaced by the recipient name."),
  Mandatory(UploadField("body", "E-mail body")),
  Button("upload", "Upload") ]) >data>
  (data.get("from"),
   data.get("subject"),
   data.get("body").getString(),
   parseRecipients(data.get("recipients").getString()))

def sendOne(from, subject, body, (name,email)) =
  (name, email)
  | SendMailFrom(from, email, subject, body.replace("{{NAME}}", name)) >>
    stop

val (from, subject, body, recipients) = getData()
each(recipients) >recipient>
( recipient
| sendOne(from, subject, body, recipient) >>
  stop
)

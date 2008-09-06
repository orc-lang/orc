include "net.inc"

val mailer = MailerFactory("orc/orchard/mail.properties")
val to = Prompt("To:")
val subject = Prompt("Subject:")
val body = Prompt("Body:")

SendMail(mailer, to, subject, body) >> "DONE"

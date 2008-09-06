include "net.inc"

{-
This program will send an email to you
and then print the body of your response.
-}

val mailer = MailerFactory("orc/orchard/mail.properties")

Prompt("Your email address:") >to>
MailQuery(mailer, to, "Orc Query", "Please reply.") >get>
"Your response: " + get()

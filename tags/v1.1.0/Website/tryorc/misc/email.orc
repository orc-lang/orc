-- Send a mail to someone
include "mail.inc"

val to = Prompt("To:")
val subject = Prompt("Subject:")
val body = Prompt("Body:")

SendMail(to, subject, body) >> "DONE"

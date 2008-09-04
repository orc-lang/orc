include "net.inc"

val TO = "test@example.com"

val mailer = MailerFactory("mail.properties")
val get = MailQuery(mailer, TO, "query", "please respond")
get()
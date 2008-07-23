val user = "USER"
val pass = "PASS"

val mymail = GMail.withProperties(
	"mail.from", cat(user, "@gmail.com"),
	"mail.user", cat(user, "@gmail.com"),
	"mail.password", pass,
	"mail.from.user", user)

MailQuery(mymail, "test@example.com",
	"hi from orc", "please respond") >r>
r()
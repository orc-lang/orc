val mymail = GMail.withProperties(
	"mail.from", "orchardserver@gmail.com",
	"mail.user", "orchardserver@gmail.com",
	"mail.password", "ckyogack",
	"mail.from.user", "orchardserver")

MailQuery(mymail, "adrian@sixfingeredman.net",
	"hi from orc", "please respond") >r>
r()
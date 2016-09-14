include "net.inc"

val server = ServletServer(8080)
val servlet = server.newServlet(["/orc"])

repeat(servlet.get) >ctx> (
	Println("Orc handling") |
	
	(ctx.getResponse().getOutputStream().println("Orc response") >>
	ctx.complete())
)

site cat = orc.lib.str.Cat
site print = orc.lib.str.Print
site println = orc.lib.str.Println

site random = orc.lib.util.Random
site UUID = orc.lib.util.UUID

site localhost = orc.lib.net.Localhost
site Mail = orc.lib.net.Mail

def SendMail(from, to, subject, body, smtp_server) =
	val message = Mail.message(subject, body, to, from)
	val transport = Mail.transport(cat("smtp://", smtp_server))
	transport.connect() >>
	transport.send(message) >>
	transport.close()

site Buffer = orc.lib.state.Buffer
site SyncChannel = orc.lib.state.SyncChannel
site Cell = orc.lib.state.Cell
site Ref = orc.lib.state.Ref

site Webservice = orc.runtime.sites.Webservice

site parseInt = orc.lib.str.AtoI
site parseBool = orc.lib.str.AtoB
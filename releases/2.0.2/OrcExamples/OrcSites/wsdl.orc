import site Webservice = "orc.lib.net.Webservice"
{-
Find documentation of this service at:
http://www.xmethods.net/ve2/WSDLRPCView.po?
key=uuid:BF3EFCDD-FCD4-8867-3AAC-068985E7CB89
-}
val service = Webservice(
  "http://www.ebob42.com/cgi-bin/"
  + "Romulan.exe/wsdl/IRoman")
service.intToRoman(451)
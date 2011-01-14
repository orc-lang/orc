site REST = orc.lib.web.REST
site XML = orc.lib.xml.ReadXML
site Element = orc.lib.xml.XmlElementSite
site Text = orc.lib.xml.XmlTextSite
 
val Fourmilab = REST("https://www.fourmilab.ch/cgi-bin/Hotbits")

def HotRandom(n) =
  val query = {. nbytes = n, fmt = "xml" .}
  Fourmilab(query)

{-
attributes: xml."name"
children: xml("name")

(xml("name"), xml("addr"))
-}
    
XML(HotRandom(3)) >Element("hotbits",_,children)>
each(children) >Element("random-data", _, [Text(bits)])>
bits


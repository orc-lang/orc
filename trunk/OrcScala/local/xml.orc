site Element = orc.lib.xml.XmlElementSite
site Text = orc.lib.xml.XmlTextSite

def content(Element(_,_,[Text(s)])) = s

site XML = orc.lib.xml.ReadXML

val directory = 
  XML("<directory>"
      + "<name>John Do</name>"
      + "<name>Jane Re</name>"
      + "<name>Jeung Mi</name>"
    + "</directory>")

each(directory("name")) >nameNode> content(nameNode)

{-
-- construct some xml --
val br = Element("br", {. .}, [])
val xml = Element("a", {. href = "http://orc.csres.utexas.edu" .}, [br, Text("Orc"), br])

-- test print the xml --
val _ = println(xml.toString())

-- pattern matching on xml --
xml >Element(tag, attr, children)>
( ("tag", tag)
| ("attributes", attr)
| ("children", children)
| each(children) >Text(s)> s
| each(children) >Element("br",_,_)> "line break!"
)

|

xml("br") | xml("href") | xml.href | (xml.br;false)
-}
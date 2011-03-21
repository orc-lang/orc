--
-- xml.inc -- Orc standard prelude include, XML section
-- Project OrcScala
--
-- $Id$
--
-- Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{- We use Scala's Node class as the underlying type of Orc XML trees -}
class XML = scala.xml.Node

{--
@site ReadXML(String) :: XML  

Parses a string representation of XML into a structured form
that Orc can manipulate.
--}
site ReadXML = orc.lib.xml.ReadXML

{--
@site WriteXML(XML) :: String

Serializes Orc's representation of XML to a string.
--}
site WriteXML = orc.lib.xml.WriteXML


site XMLElement = orc.lib.xml.XMLElementSite
site XMLText = orc.lib.xml.XMLTextSite
site XMLCData = orc.lib.xml.XMLCDataSite
site IsXML = orc.lib.xml.IsXMLSite


val xml = 
		def toxml(String, List[Top]) :: XML
		def toxml(tag, children) = 
		  def liftChild(Top) :: XML
		  def liftChild(x) = IsXML(x) ; XMLText("" + x)
		  XMLElement(tag, {. .}, map(liftChild, children))
		def fromxml(XML) :: (String, Top)
		def fromxml(XMLElement(tag,attr,children)) = each(children) >c> (tag,c)
		def fromxml(XMLText(s)) = s
		def fromxml(XMLCData(s)) = s
		{. apply = toxml, unapply = fromxml .}
  
val xattr =
		def toattr(XML, {. .}) :: XML
		def toattr(XMLElement(tag, attr, children), moreattr) = 
		  XMLElement(tag, attr + moreattr, children)
		
		def fromattr(XML) :: (XML, {. .})
		def fromattr(XMLElement(tag, attr, children)) =
		  (XMLElement(tag, {. .}, children), attr)
  {. apply = toattr, unapply = fromattr .}

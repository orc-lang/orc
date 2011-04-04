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

{--
XML manipulation.
--}

{- We use Scala's Node class as the underlying type of Orc XML trees -}
type XML = "orc.lib.xml.XMLType"

{--
@site ReadXML(String) :: XML  

Parses a string representation of XML into a structured form
that Orc can manipulate.
--}
site ReadXML = "orc.lib.xml.ReadXML"

{--
@site WriteXML(XML) :: String

Serializes Orc's representation of XML to a string.
--}
site WriteXML = "orc.lib.xml.WriteXML"

{--
@site XMLElement(String, {. .}, List[XML]) :: XML  

Creates an XML element node with the given tag, attributes,
and children. 
This site may also be used for matching.
--}
site XMLElement = "orc.lib.xml.XMLElementSite"

{--
@site XMLText(String) :: XML

Creates an XML text node with the given contents. 
Encoding may occur.
This site may also be used for matching.
--}
site XMLText = "orc.lib.xml.XMLTextSite"

{--
@site XMLCData(String) :: XML

Creates an XML text node with the given contents.
Contents will not be encoded.
This site may also be used for matching.
--}
site XMLCData = "orc.lib.xml.XMLCDataSite"

{--
@site IsXML(Top) :: XML

Acts as the identity function for any XML node,
and halts silently for any non-XML argument.
--}
site IsXML = "orc.lib.xml.IsXMLSite"


{--
@def xml(String, List[XML]) :: XML

Creates an XML element with the given tag and children,
and no attributes.
May also be used for matching. When matching, the second
argument is a multimatch on each child, rather than a
match on the list of children.

@implementation
--}
val xml = 
  def toxml(String, List[Top]) :: XML
  def toxml(tag, children) = 
    def liftChild(Top) :: XML
    def liftChild(x) = IsXML(x) ; XMLText("" + x)
    XMLElement(tag, {. .}, map(liftChild, children))
  def fromxml(XML) :: Top
  def fromxml(XMLElement(tag,attr,children)) = 
    each(children) >c> 
      ( c >XMLElement(_,_,_)> (tag,c)
      | c >XMLText(s)> (tag,s)
      | c >XMLCData(s)> (tag,s) )
  def fromxml(XMLText(s)) = s
  def fromxml(XMLCData(s)) = s
  {. apply = toxml, unapply = fromxml .}
  
{--
@def xattr(XML, {. .}) :: XML

Creates a copy of the XML element, adding new
attributes as given by the record argument.
If there is a conflict, the new attributes override the old ones.
May also be used for matching.

@implementation
--}  
val xattr =
  def toattr(XML, {. .}) :: XML
  def toattr(XMLElement(tag, attr, children), moreattr) = 
    XMLElement(tag, attr + moreattr, children)

  def fromattr(XML) :: (XML, {. .})
  def fromattr(XMLElement(tag, attr, children)) =
    (XMLElement(tag, {. .}, children), attr)
  {. apply = toattr, unapply = fromattr .}


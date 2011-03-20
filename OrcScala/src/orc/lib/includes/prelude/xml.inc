--
-- xml.inc -- Orc standard prelude include, XML section
-- Project OrcScala
--
-- $Id: web.inc 2529 2011-03-09 23:25:43Z dkitchin $
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


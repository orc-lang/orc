//
// ReadXML.scala -- Scala class ReadXML
// Project OrcScala
//
// Created by dkitchin on Nov 17, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.xml


import orc.values.sites.TypedSite
import orc.types.StringType
import orc.types.SimpleFunctionType
import scala.xml.XML
import org.xml.sax.SAXException
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.SiteException
import orc.values.sites.TotalSite1Simple

/**
  * @author dkitchin
  */
class ReadXML extends TotalSite1Simple[String] with TypedSite {

  def eval(s: String): AnyRef = {
    try {
      XML.loadString(s)
    } catch {
      case e: SAXException => {
        throw new SiteException("XML parsing failed: " + e.getMessage)
      }
    }
  }

  def orcType() = SimpleFunctionType(StringType, XMLType)

}

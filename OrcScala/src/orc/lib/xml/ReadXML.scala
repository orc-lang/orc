//
// ReadXML.scala -- Scala class ReadXML
// Project OrcScala
//
// Created by dkitchin on Nov 17, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.xml

import orc.values.sites.TotalSite1
import orc.values.sites.TypedSite
import orc.types.StringType
import orc.types.SimpleFunctionType
import scala.xml.XML
import org.xml.sax.SAXException
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.SiteException

/**
  *
  * @author dkitchin
  */
class ReadXML extends TotalSite1 with TypedSite {

  def eval(arg: AnyRef): AnyRef = {
    arg match {
      case s: String => {
        try {
          XML.loadString(s)
        } catch {
          case e: SAXException => {
            throw new SiteException("XML parsing failed: " + e.getMessage)
          }
        }
      }
      case z => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
    }
  }

  def orcType() = SimpleFunctionType(StringType, XMLType)

}

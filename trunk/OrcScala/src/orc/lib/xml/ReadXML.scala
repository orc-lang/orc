//
// Clazz.scala -- Scala class/trait/object Clazz
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 17, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.xml

import orc.values.sites.PartialSite
import orc.values.sites.UntypedSite
import scala.xml.XML
import org.xml.sax.SAXException
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException


/**
 * 
 *
 * @author dkitchin
 */
class ReadXML extends PartialSite with UntypedSite {
  
  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    args match {
      case List(xml: String) => {
        try {
          Some(XML.loadString(xml.toString))
        }
        catch {
          case e: SAXException => {
            System.err.println("XML parsing failed: " + e.getMessage)
            None
          }
          case e => throw e
        }
      }
      case List(z) => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }
  
}
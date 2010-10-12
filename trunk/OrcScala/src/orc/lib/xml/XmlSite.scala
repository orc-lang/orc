//
// XmlSite.scala -- Scala class/trait/object XmlSite
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Sep 29, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.xml

import orc.lib.builtin.Extractable
import orc.values.sites.TotalSite
import orc.values.sites.UntypedSite
import scala.xml.Elem

/**
 * 
 *
 * @author dkitchin
 */
/*
trait XmlSite extends TotalSite with Extractable with UntypedSite {

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(tag: String, attr: OrcRecord, children: List[_]) => {
        //UnprefixedAttribute...
        //Elem(null, tag, record (as MetaData), null, nodes (as Node*) )
      }
      case List(tag: String, attr: OrcRecord, z) => {
        ArgumentTypeMismatchException(2, "List[Node]", z.getClass().toString())
      }
      case List(tag: String, z, _) => {
        ArgumentTypeMismatchException(1, "Record", z.getClass().toString())
      }
      case List(z, _, _) => {
        ArgumentTypeMismatchException(0, "String", z.getClass().toString())
      }
      case _ => throw new ArityMismatchException(3, args.size)
    }
  }
  
  override def extract = new PartialSite with UntypedSite {
 
    override def evaluate(args: List[AnyRef]) = {
        args match {
          case xml: Elem => 
        }
    }
  }
  
  
}
*/
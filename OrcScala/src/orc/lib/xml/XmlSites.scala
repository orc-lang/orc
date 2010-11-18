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
import orc.values.OrcTuple
import orc.values.OrcRecord
import orc.values.sites.TotalSite
import orc.values.sites.TotalSite1
import orc.values.sites.PartialSite
import orc.values.sites.UntypedSite
import scala.xml._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

/**
 * 
 * XML elements. These sites are not namespace aware. Construction defaults
 * to the empty namespace, and matching discards namespace information.
 *
 * @author dkitchin
 */

class XmlElementSite extends TotalSite with Extractable with UntypedSite {

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(tag: String, attr: OrcRecord, children: List[_]) => {
        val metadata = attr.entries.foldRight[MetaData](Null) { case ((k,v),rest) => new UnprefixedAttribute(k, v.toString, rest) }
        val childNodes = for (c <- children) yield c.asInstanceOf[Node]
        new Elem(null, tag, metadata, TopScope, childNodes: _*)
      }
      case List(tag: String, attr: OrcRecord, z) => {
        throw new ArgumentTypeMismatchException(2, "List[Node]", z.getClass().toString())
      }
      case List(tag: String, z, _) => {
        throw new ArgumentTypeMismatchException(1, "Record", z.getClass().toString())
      }
      case List(z, _, _) => {
        throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
      }
      case _ => throw new ArityMismatchException(3, args.size)
    }
  }
  
  override def extract = 
    new PartialSite with UntypedSite {
      override def evaluate(args: List[AnyRef]) = {
        args match {
          case List(xml: Elem) => {
            val tag = xml.label
            val attr = OrcRecord(xml.attributes.asAttrMap)
            val children = xml.child.toList
            Some(OrcTuple(List(tag, attr, children)))
          }
          case _ => None
        }
      }
    }
  
  override def name = "Element"
  
}


class XmlTextSite extends TotalSite1 with Extractable with UntypedSite {

  def eval(x: AnyRef): AnyRef = {
    x match {
      case data: String => new Text(data)
      case z => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
    }
  }
  
  override def extract = 
    new PartialSite with UntypedSite {
      override def evaluate(args: List[AnyRef]) = {
        args match {
          case List(xml: Text) => {
            Some(xml._data)
          }
          case _ => None
        }
      }
  }
  
  override def name = "Text"
  
}

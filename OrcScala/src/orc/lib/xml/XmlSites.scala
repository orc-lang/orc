//
// XmlSites.scala -- Scala classes XmlElementSite and XmlTextSite
// Project OrcScala
//
// Created by dkitchin on Sep 29, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.xml

import scala.xml.{ Elem, MetaData, Node, Null, PCData, Text, TopScope, UnprefixedAttribute }

import orc.error.runtime.ArgumentTypeMismatchException
import orc.lib.builtin.structured.ListType
import orc.types.{ EmptyRecordType, JavaObjectType, SimpleFunctionType, StringType, Top, TupleType }
import orc.values.{ OrcRecord, OrcTuple }
import orc.values.sites.{ PartialSite1, StructurePairSite, TotalSite1, TotalSite3, TypedSite }

/** XML elements. These sites are not namespace aware. Construction defaults
  * to the empty namespace, and matching discards namespace information.
  *
  * @author dkitchin
  */
/* Template for building values which act as constructor-extractor sites,
 * such as the Some site.
 */

/* We use Scala's Node class as the underlying type of Orc XML trees */
object XMLType extends JavaObjectType(classOf[scala.xml.Node])

object XMLElementSite extends StructurePairSite(XMLElementConstructor, XMLElementExtractor)

object XMLElementConstructor extends TotalSite3 with TypedSite {

  override def name = "XMLElement"

  def eval(x: AnyRef, y: AnyRef, z: AnyRef): AnyRef = {
    (x, y, z) match {
      case (tag: String, attr: OrcRecord, children: List[_]) => {
        val metadata = attr.entries.foldRight[MetaData](Null) { case ((k, v), rest) => new UnprefixedAttribute(k, v.toString, rest) }
        val childNodes = for (c <- children) yield c.asInstanceOf[Node]
        new Elem(null, tag, metadata, TopScope, true, childNodes: _*)
      }
      case (tag: String, attr: OrcRecord, a) => {
        throw new ArgumentTypeMismatchException(2, "List[Node]", a.getClass().toString())
      }
      case (tag: String, a, _) => {
        throw new ArgumentTypeMismatchException(1, "Record", a.getClass().toString())
      }
      case (a, _, _) => {
        throw new ArgumentTypeMismatchException(0, "String", a.getClass().toString())
      }
    }
  }

  def orcType() =
    SimpleFunctionType(
      List(StringType, EmptyRecordType, ListType(XMLType)),
      XMLType)

}

object XMLElementExtractor extends PartialSite1 with TypedSite {

  override def name = "XMLElement.unapply"

  override def eval(arg: AnyRef): Option[AnyRef] = {
    arg match {
      case xml: Elem => {
        val tag = xml.label
        val attr = OrcRecord(xml.attributes.asAttrMap)
        val children = xml.child.toList
        Some(OrcTuple(Array(tag, attr, children)))
      }
      case _ => None
    }
  }

  def orcType() =
    SimpleFunctionType(
      XMLType,
      TupleType(List(StringType, EmptyRecordType, ListType(XMLType))))

}

object XMLTextSite extends StructurePairSite(XMLTextConstructor, XMLTextExtractor)

object XMLTextConstructor extends TotalSite1 with TypedSite {

  override def name = "XMLText"

  def eval(x: AnyRef): AnyRef = {
    x match {
      case data: String => new Text(data)
      case z => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
    }
  }

  def orcType() = SimpleFunctionType(StringType, XMLType)

}

object XMLTextExtractor extends PartialSite1 with TypedSite {

  override def name = "XMLText.unapply"

  override def eval(arg: AnyRef): Option[AnyRef] = {
    arg match {
      case xml: Text => Some(xml.text)
      case _ => None
    }
  }

  def orcType() = SimpleFunctionType(XMLType, StringType)
}

object XMLCDataSite extends StructurePairSite(XMLCDataConstructor, XMLCDataExtractor)

object XMLCDataConstructor extends TotalSite1 with TypedSite {

  override def name = "XMLCData"

  def eval(x: AnyRef): AnyRef = {
    x match {
      case data: String => new PCData(data)
      case z => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
    }
  }

  def orcType() = SimpleFunctionType(StringType, XMLType)

}

object XMLCDataExtractor extends PartialSite1 with TypedSite {

  override def name = "XMLCData.unapply"

  override def eval(arg: AnyRef): Option[AnyRef] = {
    arg match {
      case xml: PCData => Some(xml.text)
      case _ => None
    }
  }

  def orcType() = SimpleFunctionType(XMLType, StringType)

}

object IsXMLSite extends PartialSite1 with TypedSite {

  override def name = "IsXML"

  def eval(x: AnyRef): Option[AnyRef] = {
    x match {
      case _: Node => Some(x)
      case _ => None
    }
  }

  def orcType() = SimpleFunctionType(Top, XMLType)

}

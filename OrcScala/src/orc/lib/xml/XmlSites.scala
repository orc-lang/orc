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
import orc.values.sites._

/** XML elements. These sites are not namespace aware. Construction defaults
  * to the empty namespace, and matching discards namespace information.
  *
  * @author dkitchin
  */

/* We use Scala's Node class as the underlying type of Orc XML trees */
object XMLType extends JavaObjectType(classOf[scala.xml.Node])

object XMLElementSite extends StructurePairSite(XMLElementConstructor, XMLElementExtractor) with Serializable with LocalSingletonSite

object XMLElementConstructor extends TotalSite3Simple[String, OrcRecord, List[_]] with TypedSite with Serializable with LocalSingletonSite {

  override def name = "XMLElement"

  def eval(tag: String, attr: OrcRecord, children: List[_]): AnyRef = {
    val metadata = attr.entries.foldRight[MetaData](Null) { case ((k, v), rest) => new UnprefixedAttribute(k, v.toString, rest) }
    val childNodes = for (c <- children) yield c.asInstanceOf[Node]
    new Elem(null, tag, metadata, TopScope, true, childNodes: _*)
  }

  def orcType() =
    SimpleFunctionType(
      List(StringType, EmptyRecordType, ListType(XMLType)),
      XMLType)

}

object XMLElementExtractor extends PartialSite1Simple[AnyRef] with TypedSite with Serializable with LocalSingletonSite {

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

object XMLTextSite extends StructurePairSite(XMLTextConstructor, XMLTextExtractor) with Serializable with LocalSingletonSite

object XMLTextConstructor extends TotalSite1Simple[AnyRef] with TypedSite with Serializable with LocalSingletonSite {

  override def name = "XMLText"

  def eval(x: AnyRef): AnyRef = {
    x match {
      case data: String => new Text(data)
      case z => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
    }
  }

  def orcType() = SimpleFunctionType(StringType, XMLType)

}

object XMLTextExtractor extends PartialSite1Simple[AnyRef] with TypedSite with Serializable with LocalSingletonSite {

  override def name = "XMLText.unapply"

  override def eval(arg: AnyRef): Option[AnyRef] = {
    arg match {
      case xml: Text => Some(xml.text)
      case _ => None
    }
  }

  def orcType() = SimpleFunctionType(XMLType, StringType)
}

object XMLCDataSite extends StructurePairSite(XMLCDataConstructor, XMLCDataExtractor) with Serializable with LocalSingletonSite

object XMLCDataConstructor extends TotalSite1Simple[AnyRef] with TypedSite with Serializable with LocalSingletonSite {

  override def name = "XMLCData"

  def eval(x: AnyRef): AnyRef = {
    x match {
      case data: String => new PCData(data)
      case z => throw new ArgumentTypeMismatchException(0, "String", z.getClass().toString())
    }
  }

  def orcType() = SimpleFunctionType(StringType, XMLType)

}

object XMLCDataExtractor extends PartialSite1Simple[AnyRef] with TypedSite with Serializable with LocalSingletonSite {

  override def name = "XMLCData.unapply"

  override def eval(arg: AnyRef): Option[AnyRef] = {
    arg match {
      case xml: PCData => Some(xml.text)
      case _ => None
    }
  }

  def orcType() = SimpleFunctionType(XMLType, StringType)

}

object IsXMLSite extends PartialSite1Simple[AnyRef] with TypedSite with Serializable with LocalSingletonSite {

  override def name = "IsXML"

  def eval(x: AnyRef): Option[AnyRef] = {
    x match {
      case _: Node => Some(x)
      case _ => None
    }
  }

  def orcType() = SimpleFunctionType(Top, XMLType)

}

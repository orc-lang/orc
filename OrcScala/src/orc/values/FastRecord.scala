//
// FastRecord.scala -- Scala class FastRecord
// Project OrcScala
//
// Created by amp on Aug, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values

import orc.{ OrcRuntime, Accessor }
import orc.values.sites.AccessorValue
import orc.run.distrib.DOrcMarshalingReplacement
import orc.values.FastRecord

class FastRecordFactory(_members: Seq[String]) {
  def this(_members: Array[String]) = this(_members.toSeq)

  /** The canonical member ordering for FastRecords produced by this factory.
    */
  val members: Array[Field] = _members.map(Field(_)).toArray

  def apply(values: Seq[AnyRef]) = {
    FastRecord(members, values.toArray)
  }
  def apply(values: Array[AnyRef]) = {
    FastRecord(members, values)
  }
}

object FastRecord {
  final class AccessorImpl(members: Array[Field], index: Int) extends Accessor {
    def canGet(target: AnyRef): Boolean = {
      target.isInstanceOf[FastRecord] &&
        (target.asInstanceOf[FastRecord].members eq members)
    }
    def get(target: AnyRef): AnyRef = {
      target.asInstanceOf[FastRecord].values(index)
    }
  }
}

case class FastRecord(members: Array[Field], values: Array[AnyRef]) extends AccessorValue with DOrcMarshalingReplacement {

  def getAccessor(runtime: OrcRuntime, field: Field): Accessor = {
    val index = members.indexOf(field)
    new FastRecord.AccessorImpl(members, index)
  }

  override def isReplacementNeededForMarshaling(marshalValueWouldReplace: AnyRef => Boolean): Boolean =
    values.exists(marshalValueWouldReplace)

  override def replaceForMarshaling(marshaler: AnyRef => AnyRef): AnyRef =
    FastRecord(members, values map marshaler)

  override def isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace: AnyRef => Boolean): Boolean =
    values.exists(unmarshalValueWouldReplace)

  override def replaceForUnmarshaling(unmarshaler: AnyRef => AnyRef): AnyRef =
    FastRecord(members, values map unmarshaler)
}

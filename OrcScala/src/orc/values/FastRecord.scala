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
import orc.run.distrib.DOrcMarshalingReplacement

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

object FastObject {
  final class AccessorImpl(members: Array[Field], index: Int) extends Accessor {
    def canGet(target: AnyRef): Boolean = {
      target.isInstanceOf[FastRecord] &&
        (target.asInstanceOf[FastRecord].members eq members)
    }
    def get(target: AnyRef): AnyRef = {
      target.asInstanceOf[FastObject].values(index)
    }
  }

  def members(members: String*): Array[Field] = members.map(Field(_)).toArray
}

abstract class FastObject(members: Array[Field]) extends HasMembers with DOrcMarshalingReplacement {
  protected val values: Array[AnyRef]

  final def getAccessor(runtime: OrcRuntime, field: Field): Accessor = {
    val index = members.indexOf(field)
    new FastObject.AccessorImpl(members, index)
  }

  override def isReplacementNeededForMarshaling(marshalValueWouldReplace: AnyRef => Boolean): Boolean =
    values.exists(marshalValueWouldReplace)

  // FIXME: By rights, this should return a new instance of this.getClass. But that's tricky to do. So rewrap in a FastRecord. It should be indistiguishable.
  override def replaceForMarshaling(marshaler: AnyRef => AnyRef): AnyRef =
    FastRecord(members, values map marshaler)

  override def isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace: AnyRef => Boolean): Boolean =
    values.exists(unmarshalValueWouldReplace)

  // FIXME: By rights, this should return a new instance of this.getClass. But that's tricky to do. So rewrap in a FastRecord. It should be indistiguishable.
  override def replaceForUnmarshaling(unmarshaler: AnyRef => AnyRef): AnyRef =
    FastRecord(members, values map unmarshaler)
}

case class FastRecord(members: Array[Field], values: Array[AnyRef]) extends FastObject(members)

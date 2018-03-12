//
// Types.scala -- Scala object Types
// Project OrcScala
//
// Created by dkitchin on Nov 30, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values.sites.compatibility

import orc.types._
import orc.lib.builtin.structured.ListType
import orc.lib.builtin.structured.OptionType
import orc.compile.typecheck.Typeloader

/** Type-building interface for Java sites to build instances of
  * orc.types.Type
  *
  * @author dkitchin
  */
@deprecated("Use Scala API directly.", "3.0")
object Types {

  def top = Top
  def bot = Bot
  def bool = BooleanType
  def signal = SignalType
  def number = NumberType
  def integer = IntegerType
  def string = StringType

  def java(C: Class[_]) = Typeloader.liftJavaType(C)

  def list(t: Type) = ListType(t)
  def option(t: Type) = OptionType(t)

  def function(r: Type) = FunctionType(Nil, Nil, r)
  def function(a: Type, r: Type) = FunctionType(Nil, List(a), r)
  def function(a0: Type, a1: Type, r: Type) = FunctionType(Nil, List(a0, a1), r)

  def overload(t: CallableType, u: CallableType) = OverloadedType(List(t, u))
  def overload(t: CallableType, u: CallableType, v: CallableType) = OverloadedType(List(t, u, v))
  def overload(t: CallableType, u: CallableType, v: CallableType, w: CallableType) = OverloadedType(List(t, u, v, w))
  def overload(t: CallableType, u: CallableType, v: CallableType, w: CallableType, x: CallableType) = OverloadedType(List(t, u, v, w, x))
  def overload(ot: OverloadedType, u: CallableType) = OverloadedType(u :: ot.alternatives)

  val emptyRecord = RecordType(Nil.toMap)

  def addField(baseType: RecordType, label: String, entryType: Type): RecordType = {
    RecordType(baseType.entries + ((label, entryType)))
  }

  // Creates a new record type with one entry, "apply", containing this callable type
  def dotSite(baseType: CallableType): RecordType = {
    RecordType(List(("apply", baseType)).toMap)
  }

}

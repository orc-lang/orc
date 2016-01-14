//
// ArrayType.scala -- Scala object ArrayType
// Project OrcScala
//
// Created by dkitchin on Dec 1, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.state.types

import orc.types._
import orc.error.compiletime.typing._

/**
  * @author dkitchin
  */
object ArrayType extends SimpleTypeConstructor("Array", Invariant) {

  def getBuilder: Type = {

    val X = new TypeVariable()
    val makeEmpty = FunctionType(List(X), List(IntegerType), this(X))

    val Y = new TypeVariable()
    val makeFull = FunctionType(List(Y), List(IntegerType, StringType), this(Y))

    OverloadedType(List(makeEmpty, makeFull))
  }

  override def instance(ts: List[Type]) = {
    val List(t) = ts
    new RecordType(
      "apply" -> SimpleFunctionType(IntegerType, RefType(t)),
      "length" -> RefType(IntegerType))
  }

}

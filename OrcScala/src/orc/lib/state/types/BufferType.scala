//
// BufferType.scala -- Scala object BufferType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Dec 1, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.state.types

import orc.types._
import orc.error.compiletime.typing._
import orc.lib.builtin.ListType

/**
 * 
 *
 * @author dkitchin
 */

object BufferType extends SimpleTypeConstructor("Buffer", Invariant) {
  
  def getBuilder: Type = {
    val X = new TypeVariable()
    FunctionType(List(X), Nil, this(X)) 
  }
  
  override def instance(ts: List[Type]) = { 
    val List(t) = ts
    new RecordType(
      "get" -> SimpleFunctionType(t),
      "getD" -> SimpleFunctionType(t),
      "put" -> SimpleFunctionType(t, SignalType),
      "close" -> SimpleFunctionType(SignalType),
      "closeD" -> SimpleFunctionType(SignalType),
      "isClosed" -> SimpleFunctionType(BooleanType),
      "getAll" -> SimpleFunctionType(ListType(t))
    ) 
  }
   
}
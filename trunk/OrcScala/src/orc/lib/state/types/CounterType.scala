//
// CounterType.scala -- Scala object CounterType
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

/**
 * 
 *
 * @author dkitchin
 */
object CounterType extends RecordType(
  "inc" -> SimpleFunctionType(SignalType),
  "dec" -> SimpleFunctionType(SignalType),
  "onZero" -> SimpleFunctionType(SignalType),
  "value" -> SimpleFunctionType(IntegerType)
) {
 
  override def toString = "Counter"
  
  def getBuilder: Type = {
    val makeEmpty = SimpleFunctionType(this)
    val makeFull = SimpleFunctionType(IntegerType, this)
    OverloadedType(List(makeEmpty, makeFull))
  }
 
}

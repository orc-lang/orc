//
// CellType.scala -- Scala object CellType
// Project OrcScala
//
// $Id$
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
  *
  * @author dkitchin
  */

object CellType extends SimpleTypeConstructor("Cell", Invariant) {

  def getBuilder: Type = {
    val X = new TypeVariable()
    FunctionType(List(X), Nil, this(X))
  }

  override def instance(ts: List[Type]) = {
    val List(t) = ts
    new RecordType(
      "read" -> SimpleFunctionType(t),
      "readD" -> SimpleFunctionType(t),
      "write" -> SimpleFunctionType(t, SignalType))
  }
}

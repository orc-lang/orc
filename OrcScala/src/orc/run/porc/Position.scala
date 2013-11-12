//
// PorcPosition.scala -- Scala class/trait/object PorcPosition
// Project OrcScala
//
// $Id$
//
// Created by amp on Nov 10, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porc

import scala.util.parsing.input.Position
import scala.util.parsing.input.NoPosition

/** The position represents a position in the Orc program.
  * The instructionNumber is the position in the Porc program.
  *
  * @author amp
  */
case class PorcPosition(position: Position, instructionNumber: Int, variableName: Option[String])

class PorcDebugTable(private val debugMap: Map[Int, PorcPosition]) {
  def apply(e: Expr) = debugMap.getOrElse(e.identityHashCode, PorcDebugTable.defaultPosition)
  def apply(v: Var) = debugMap.getOrElse(v.identityHashCode, PorcDebugTable.defaultPosition)
  
  def get(e: Expr) = debugMap.get(e.identityHashCode)
  def get(v: Var) = debugMap.get(v.identityHashCode)

  def ++(o: PorcDebugTable) = {
    PorcDebugTable(debugMap ++ o.debugMap)
  }
}

object PorcDebugTable {
  val defaultPosition = PorcPosition(NoPosition, -1, None)

  def apply(exprMap: Map[Int, PorcPosition]): PorcDebugTable = {
    new PorcDebugTable(exprMap)
  }

  def apply(): PorcDebugTable = {
    PorcDebugTable(Map())
  }
}
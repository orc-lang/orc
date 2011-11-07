//
// SwappableASTs.scala -- Scala trait/object SwappableASTs
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Oct 23, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import scala.collection.immutable.List
import orc.OrcExecutionOptions
import orc.ast.oil.nameless.Expression
import orc.ast.oil.nameless.Def
import orc.run.Orc

/**
 * Trait that mixes-in update access to Orc runtime engine objects that
 * hold references to OIL AST nodes.
 *
 * @author jthywiss
 */
trait SwappableASTs extends Orc {
}

object SwappableASTs {
  def setExecutionNode(e: SwappableASTs#Execution, node: Expression) { Console.err.println(">>SwappableASTs.setExecutionNode"); e._node = node }
  def setExecutionOptions(e: SwappableASTs#Execution, options: OrcExecutionOptions) { Console.err.println(">>SwappableASTs.setExecutionOptions"); e._options = options }
  def setClosureDef(c: SwappableASTs#Closure, defs: List[Def]) { Console.err.println(">>SwappableASTs.setClosureDef"); c._defs = defs }
  def setSequenceFrameNode(sf: SwappableASTs#SequenceFrame, node: Expression) { Console.err.println(">>SwappableASTs.setSequenceFrameNode"); sf._node = node }
  def setFunctionFrameCallpoint(ff: SwappableASTs#FunctionFrame, callpoint: Expression) { Console.err.println(">>SwappableASTs.setFunctionFrameCallpoint"); ff._callpoint = callpoint }
}

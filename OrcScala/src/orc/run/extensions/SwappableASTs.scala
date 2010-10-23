//
// SwappableASTs.scala -- Scala trait/object SwappableASTs
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Oct 23, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import scala.collection.immutable.List
import orc.run.Orc
import orc.ast.oil.nameless.Def
import orc.ast.oil.nameless.Expression

/**
 * Trait that mixes-in update access to Orc runtime engine objects that
 * hold references to OIL AST nodes.
 *
 * @author jthywiss
 */
trait SwappableASTs extends Orc {
}

object SwappableASTs {
  def setClosureDef(c: SwappableASTs#Closure, defs: List[Def]) { c._defs = defs }
  def setSequenceFrameNode(sf: SwappableASTs#SequenceFrame, node: Expression) { sf._node = node }
  def setFunctionFrameCallpoint(ff: SwappableASTs#FunctionFrame, callpoint: Expression) { ff._callpoint = callpoint }
}

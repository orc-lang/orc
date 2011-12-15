//
// Closure.scala -- Scala class Closure
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.ast.oil.nameless.Def

/** @author dkitchin
  */
case class Closure(private[run] var _defs: List[Def], pos: Int, lexicalContext: List[Binding]) {

  def defs = _defs

  def code: Def = defs(pos)

  def context: List[Binding] = {
    val fs =
      for (i <- defs.indices) yield {
        BoundValue(Closure(defs, i, lexicalContext))
      }
    fs.toList.reverse ::: lexicalContext
  }

}

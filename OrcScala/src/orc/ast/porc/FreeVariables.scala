//
// FreeVariables.scala -- Scala class/trait/object FreeVariables
// Project OrcScala
//
// Created by amp on Jun 2, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

/**
  * @author amp
  */
trait FreeVariables {
  this: PorcAST =>
  lazy val freeVars: Set[Variable] = {
    this match {
      case v: Variable => Set(v)
      case _ =>
        this.subtrees.flatMap(_.freeVars).toSet -- this.boundVars
    }
  }
}

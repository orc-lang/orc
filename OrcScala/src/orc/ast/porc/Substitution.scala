//
// Substitution.scala -- Scala trait Substitution
// Project OrcScala
//
// Created by dkitchin on Jul 13, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

/** Direct substitutions on named ASTs.
  *
  * @author dkitchin
  */

trait Substitution[X <: PorcAST] {
  self: PorcAST =>

  def substAll(subs: collection.Map[Variable, Argument]): X = Substitution.allArgs(subs)(this.toZipper()).asInstanceOf[X]
}

object Substitution {
  def allArgs(subs: scala.collection.Map[Variable, Argument]) =
    new Transform {
      override def onArgument = {
        case x: Variable.Z => {
          if (subs.isDefinedAt(x.value)) { subs(x.value) } else { x.value }
        }
      }
    }
}

//
// PorcInfixNotation.scala -- Scala class/trait/object PorcInfixNotation
// Project OrcScala
//
// Created by amp on May 27, 2013.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

/**
  * @author amp
  */

trait PorcInfixValue {
  this: Argument =>
  def apply(arg: Argument*) = CallContinuation(this, arg.toSeq)
}
trait PorcInfixExpr {
  this: Expression =>
  def :::(f: Expression): Expression = Sequence(Seq(f, this))
}

object PorcInfixNotation {
  implicit val reflectiveCalls = scala.language.reflectiveCalls

  def let(defs: (Variable, Expression)*)(body: Expression): Expression = 
    if (defs.isEmpty) body else Let(defs.head._1, defs.head._2, let(defs.tail: _*)(body))
  def continuation(arg: Variable*)(body: Expression): Expression = Continuation(arg.toSeq, body)
}

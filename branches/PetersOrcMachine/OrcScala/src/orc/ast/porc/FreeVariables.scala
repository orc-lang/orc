//
// FreeVariables.scala -- Scala class/trait/object FreeVariables
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 2, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import scala.collection.mutable

/**
  *
  * @author amp
  */
trait FreeVariables {
  this: Expr =>
  lazy val freevars: Set[Var] = {
    val s = mutable.Set[Var]()
    (new ContextualTransform.NonDescending {
      override def onVar = {
        case (v : Var) in _ => s += v; v
      }
    })(this)
    s.toSet
  }
}
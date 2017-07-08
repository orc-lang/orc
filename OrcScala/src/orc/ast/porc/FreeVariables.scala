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

import scala.collection.mutable

/**
  * @author amp
  */
trait FreeVariables {
  this: Expression =>
  lazy val freeVars: Set[Variable] = {
    val s = mutable.Set[Variable]()
    (new Transform {
      override def onArgument = {
        case v: Variable.Z if !v.contextBoundVars.contains(v.value) =>
          s += v.value
          v.value
      }
    })(this.toZipper())
    //Logger.finest(s"$this has free vars ${s.toSet}")
    s.toSet
  }
}

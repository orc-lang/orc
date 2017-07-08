//
// Resolver.scala -- Scala trait Resolver
// Project OrcScala
//
// Created by amp on Jan 18, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.FutureReadHandle
import java.lang.AssertionError

/** @author amp
  */
trait Resolver extends Blockable {

  /** Attempt to resolve a binding to a value.
    * When the binding resolves to v, call k(v).
    * (If it is already resolved, k is called immediately)
    *
    * If the binding resolves to a halt, halt this token.
    */
  protected def resolve(b: Binding)(k: AnyRef => Unit) {
    resolveOptional(b) {
      case Some(v) => k(v)
      case None => halt()
    }
  }

  /** Attempt to resolve a binding to a value.
    * When the binding resolves to v, call k(Some(v)).
    * (If it is already resolved, k is called immediately)
    *
    * If the binding resolves to a halt, call k(None).
    *
    * Note that resolving a closure also encloses its context.
    */
  protected def resolveOptional(b: Binding)(k: Option[AnyRef] => Unit) {
    b match {
      case BoundValue(v) =>
        k(Some(v))
      case BoundStop => k(None)
      case BoundReadable(g) => {
        pushContinuation(k)
        g.read(this)
      }
    }
  }


  /** Store a continuation that will be run when this resolver is unblocked with a value.
    * None means stop. Some(v) means the value v.
    */
  protected def pushContinuation(k: (Option[AnyRef] => Unit)): Unit
}

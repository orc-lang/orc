//
// Guarded.scala -- Scala trait Guarding
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 4, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named

/** @author dkitchin
  */
trait Guarding {
  self: Expression =>

  def checkGuarded(unguardedRecursion: Expression => Unit) { checkGuarded(Nil, unguardedRecursion) }

  /* The context contains only those variables which would be
   * considered recursive call targets in the current context.
   */
  def checkGuarded(context: List[BoundVar], unguardedRecursion: Expression => Unit): Boolean = {
    def check(e: Expression) = e.checkGuarded(context, unguardedRecursion)
    this match {
      case Stop() => true
      case a: Argument => false
      case Call(target, _, _) => {
        if (context contains target) {
          unguardedRecursion(this); false
        } else {
          /* This is a liberal approximation and will generate false negatives.
           * Not all calls are truly guarding; only calls to sites or to guarded
           * functions or closures are actually guarding. However, since this
           * is just a sanity check, not a safety guarantee, it seems appropriate
           * to incur false negatives to repress a proliferation of false positives.
           */
          true
        }
      }
      case left || right => {
        val l = check(left)
        val r = check(right)
        l && r
      }
      case left > x > right => {
        val l = check(left)
        val r = right.checkGuarded(if (l) { Nil } else context, unguardedRecursion)
        l || r
      }
      case Graft(x, value, body) => {
        val l = check(value)
        val r = check(body)
        l && r
      }
      case left ow right => {
        val l = check(left)
        val r = right.checkGuarded(if (l) { Nil } else context, unguardedRecursion)
        l || r
      }
      case Trim(body) => check(body)
      case DeclareCallables(defs, body) => {
        val newcontext = (defs map { _.name }) ::: context
        val _ = for (d <- defs) yield { d.body.checkGuarded(newcontext, unguardedRecursion) }
        check(body)
      }
      case DeclareType(_, _, body) => check(body)
      case HasType(body, _) => check(body)
      case VtimeZone(timeOrder, body) => check(body)
    }
  }

}

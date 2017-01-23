//
// Guarded.scala -- Scala trait Guarding
// Project OrcScala
//
// Created by dkitchin on Aug 4, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named

import orc.compile.Logger

/** @author dkitchin
  */
trait Guarding {
  self: Expression =>

  def checkGuarded(unguardedRecursion: Expression => Unit) { checkGuarded(Nil, unguardedRecursion) }

  /* The context contains only those variables which would be
   * considered recursive call targets in the current context.
   *
   * Calls unguardedRecursion if an instance of unguarded recursion is found.
   *
   * Returns: True if the expression is "guarding", meaning that it will not always publish
   * and whether or not it publishes represents some choice or it will publish with some delay.
   */
  def checkGuarded(context: List[BoundVar], unguardedRecursion: Expression => Unit): Boolean = {
    def check(e: Expression) = e.checkGuarded(context, unguardedRecursion)
    this match {
      case Stop() => true
      case a: Argument => false
      case Call(target, _, _) => {
        if (context contains target) {
          unguardedRecursion(this)
          false
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
      case Parallel(left, right) => {
        val l = check(left)
        val r = check(right)
        l && r
      }
      case Sequence(left, x, right) => {
        val l = check(left)
        val r = right.checkGuarded(if (l) { Nil } else context, unguardedRecursion)
        l || r
      }
      case Graft(x, value, body) => {
        // TODO: This will produce false positives because x may act as a guard in body.
        val l = check(value)
        val r = check(body)
        l && r
      }
      case Otherwise(left, right) => {
        val l = check(left)
        val r = right.checkGuarded(if (l) { Nil } else context, unguardedRecursion)
        l || r
      }
      case Trim(body) => check(body)
      case New(target) => {
        // This allows false negatives in cases where classes are mixed in which do not block recursion.
        target match {
          case List(c) if context contains c.name => {
            unguardedRecursion(this)
          }
          case _ => {}
        }
        false
      }
      case FieldAccess(o, f) => true
      case DeclareClasses(clss, body) => {
        //val newcontext = clss.map(_.name) ::: context
        for (c <- clss; e <- c.bindings.values) {
          // TODO: This only checks for direct self recursion. Mutual will not be caught
          // To catch mutual recursion we need to fracture classes like defs (into mutually recursive groups).
          e.checkGuarded(c.name :: context, unguardedRecursion)
        }
        check(body)
      }
      case DeclareCallables(defs, body) => {
        val newcontext = (defs map { _.name }) ::: context
        for (d <- defs) {
          d.body.checkGuarded(newcontext, unguardedRecursion)
        }
        check(body)
      }
      case DeclareType(_, _, body) => check(body)
      case HasType(body, _) => check(body)
      case Hole(_, _) => false
      case VtimeZone(timeOrder, body) => check(body)
    }
  }

}

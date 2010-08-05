//

// Guarded.scala -- Scala class/trait/object Guarded
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 4, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.oil.named

/**
 * 
 *
 * @author dkitchin
 */
trait Guarding {
  self : Expression =>
  
  def checkGuarded: Boolean = this.checkGuarded(Nil)
  
  /* The context contains only those variables which would be
   * considered recursive call targets in the current context.
   */
  def checkGuarded(context: List[BoundVar]): Boolean = {
    this match {
      case Stop() => true
      case a : Argument => false
      case Call(target, _, _) => {
        if (context contains target) {
          this emitWarning "Unguarded recursion" ; 
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
      case left || right => {
        val l = left.checkGuarded(context)
        val r = right.checkGuarded(context)
        l && r
      }
      case left > x > right => {
        val l = left.checkGuarded(context) 
        val r = right.checkGuarded(if (l) { Nil } else context)
        l || r
      }
      case left < x < right => {
        val l = left.checkGuarded(context)
        val r = right.checkGuarded(context)
        l && r
      }
      case left ow right => {
        val l = left.checkGuarded(context)
        val r = right.checkGuarded(if (l) { Nil } else context)
        l || r
      }
      case DeclareDefs(defs, body) => {
        val newcontext = (defs map { _.name }) ::: context
        val _ = for (d <- defs) yield { d.body.checkGuarded(newcontext) }
        body.checkGuarded(context)
      }
      case DeclareType(_, _, body) => body.checkGuarded(context)
      case HasType(body, _) => body.checkGuarded(context)
    }
  }
  
}
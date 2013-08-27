//
// Closure.scala -- Scala class Closure
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.ast.oil.nameless.Def
import orc.Schedulable
import orc.OrcRuntime
import orc.util.BlockableMapExtension

/** 
 * A closure that both resolves itself and represents the closure itself. This should
 * be scheduled when it is created.
 * 
 * @author dkitchin, amp
 */
class Closure(
    private[run] var _defs: List[Def], 
    index: Int, 
    private var _lexicalContext: List[Binding], 
    override val runtime: OrcRuntime) 
      extends Schedulable 
        with Blocker with Blockable with Resolver {
  import Closure._
  import BlockableMapExtension._

  private var stack : List[Option[AnyRef] => Unit] = Nil

  private def pop() = {
    val top = stack.head
    stack = stack.tail
    top
  }
  private def push(k : (Option[AnyRef] => Unit)) = stack = k :: stack
  protected def pushContinuation(k : (Option[AnyRef] => Unit)) = push(k)

  // State is used for the blocker side
  private var state : ClosureState = Started
  
  // waitlist is effectively the state of the blocker side.
  private var waitlist : List[Blockable] = Nil // This should be empty at any time state = Resolved
  
  def defs = _defs

  def code: Def = defs(index)
  
  def lexicalContext = _lexicalContext
  
  override def toString = synchronized { "Closure@" + ## + (code.body.pos, lexicalContext, state, stack) }

  // FIXME: This should work but it produces duplicate versions of closures and I don't see why 
  // we should do that. The resolution will be fast because the lexical context will already 
  // have been resolved. But still.

  // This is a lazy val because it the data in the returned object always has 
  // the same meaning even if it is mutable. So we can safely reuse the result.
  lazy val context: List[Binding] = synchronized {
    val fs =
      for (i <- defs.indices) yield {
        val c = new Closure(defs, i, lexicalContext, runtime)
        runtime.stage(c)
        // TODO: Check if lexialContext has any futures or closures that are not resolved. if not we can make a BoundValue instead.
        //This does not explain the crashes however.
        BoundClosure(c)
      }
    fs.toList.reverse ::: lexicalContext
  }
  
  /** Create a new Closure object whose lexical bindings are all resolved and replaced.
    * Such a closure will have no references to any group.
    * This object is then passed to the continuation.
    */
  private def enclose(bs: List[Binding])(k: List[Binding] => Unit): Unit = {
    def resolveBound(b: Binding)(k: Binding => Unit) =
      resolveOptional(b) {
        case Some(v) => k(BoundValue(v))
        case None => k(BoundStop)
      }
    bs.blockableMap(resolveBound)(k)
  }

  
  /**
   * Execute the resolution process of this Closure. This should be called by the scheduler.
   */
  def run() = synchronized {
    state match {
      case Started => {
        // Start the resolution process
        enclose(_lexicalContext){ newContext => 
          assert(stack.size == 0) // Check for a bug that existed before and was non-deterministic
          //assert(_lexicalContext.size == newContext.size) // FIXME: Silly debugging check
          _lexicalContext = newContext
          state = Resolved
          waitlist foreach runtime.stage
          waitlist = Nil
        }
      }
      case Blocked(b) => {
        b.check(this)
      }
      case Resolved => throw new AssertionError("Closure scheduled in bad state: " + state)
    }
  }
  
  //// Blocker Implementation
  
  def check(t: Blockable) = synchronized {
    state match {
      case Resolved => t.awakeValue(this)
      case _ => throw new AssertionError("Closure.check called in bad state: " + state)
    }
  }
  
  def read(t: Blockable) = synchronized {
    state match {
     case Resolved => t.awakeValue(this)
     case _ => {
        t.blockOn(this)
        waitlist ::= t
      }
    }
  }

  //// Blockable Implementation
  
  private def handleValue(v : Option[AnyRef]) {
    pop()(v) // Pop and call the old top of the stack on the value. The pop happens first. That's important.
  }
  
  def awakeValue(v : AnyRef) = handleValue(Some(v))
  def awakeStop() = handleValue(None)
  
  def blockOn(b : Blocker) {
    assert(state != Resolved)
    state = Blocked(b)
  }
}

object Closure {
  sealed trait ClosureState
  case class Blocked(blocker : Blocker) extends ClosureState
  case object Resolved extends ClosureState
  case object Started extends ClosureState
}

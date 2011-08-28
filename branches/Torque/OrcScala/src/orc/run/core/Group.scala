//
// Group.scala -- Scala class/trait/object Group
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import scala.collection.mutable
import orc.error.runtime.TokenLimitReachedError
import orc.OrcRuntime
import orc.Schedulable

/**
 * 
 * A Group is a structure associated with dynamic instances of an expression,
 * tracking all of the executions occurring within that expression.
 * Different combinators make use of different Group subclasses.
 *
 * @author dkitchin
 */

trait Group extends GroupMember with Schedulable {
  
  def publish(t: Token, v: AnyRef): Unit
  def onHalt(): Unit
  def run(): Unit

  override val nonblocking = true
  
  val runtime: OrcRuntime
  
  var members: mutable.Set[GroupMember] = mutable.Set()
  var alive = true

  def halt(t: Token) = synchronized { remove(t) }

  def kill() = synchronized {
    if (alive) {
      alive = false
      for (m <- members) { runtime.schedule(m) }
      // TODO: null out members
    }
  }
  
  def isKilled() = { !alive }

  def suspend() = synchronized {
    for (m <- members) m.suspend()
  }

  def resume() = synchronized {
    for (m <- members) m.resume()
  }

  def add(m: GroupMember) {
    synchronized {
      members.add(m)
    }
    m match {
      case t: Token if (root.options.maxTokens > 0) => {
        if (root.tokenCount.incrementAndGet() > root.options.maxTokens)
          throw new TokenLimitReachedError(root.options.maxTokens)
      }
      case _ => {}
    }
  }

  def remove(m: GroupMember) {
    synchronized {
      members.remove(m)
      if (members.isEmpty) { onHalt }
    }
    m match {
      case t: Token if (root.options.maxTokens > 0) => root.tokenCount.decrementAndGet()
      case _ => {}
    }
  }

  def inhabitants: List[Token] =
    members.toList flatMap {
      case t: Token => List(t)
      case g: Group => g.inhabitants
    }

  /* Find the root of this group tree. */
  val root: Execution

}
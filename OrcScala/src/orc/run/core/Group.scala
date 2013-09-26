//
// Group.scala -- Scala trait Group
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

import scala.collection.mutable

import orc.OrcRuntime
import orc.error.runtime.TokenLimitReachedError

/** A Group is a structure associated with dynamic instances of an expression,
  * tracking all of the executions occurring within that expression.
  * Different combinators make use of different Group subclasses.
  *
  * @author dkitchin
  */
trait Group extends GroupMember {

  override val nonblocking = true

  val members: mutable.Buffer[GroupMember] = new mutable.ArrayBuffer(2)

  val runtime: OrcRuntime

  /** Find the root of this group tree. */
  val root: Execution

  private var alive = true

  /** An expensive walk-to-root check for alive state */
  override def checkAlive(): Boolean = synchronized { alive }

  def publish(t: Token, v: Option[AnyRef]): Unit
  def onHalt(): Unit
  def run(): Unit

  def halt(t: Token) = synchronized { remove(t) }

  def kill() = synchronized {
    if (alive) {
      alive = false
      for (m <- members) {
        runtime.stage(m)
        /* Optimization: assume Tokens do not remove themselves from Groups */
        if (root.options.maxTokens > 0 && m.isInstanceOf[Token]) root.tokenCount.decrementAndGet()
      }
      // TODO: members.clear() ?  Only needed for Tokens
    }
  }

  /*
   * Note: This is a local live check.
   * A global check requires a linear-time
   * ascension of the group tree.
   */
  def isKilled() = synchronized { !alive }

  def suspend() = synchronized {
    for (m <- members) m.suspend()
  }

  def resume() = synchronized {
    for (m <- members) m.resume()
  }

  def add(m: GroupMember) {
    synchronized {
      if (!alive) {
        m.kill()
      }
      assert(!members.contains(m), s"Double Group.add of $m")
      members += m
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
      members -= m
      if (members.isEmpty) { onHalt() }
    }
    m match {
      /* NOTE: We rely on the optimization that Tokens are not removed from their group when killed.
       * Thus, there is no kill-halt multiple remove issue for tokens.  */
      case t: Token if (root.options.maxTokens > 0) => root.tokenCount.decrementAndGet()
      case _ => {}
    }
  }

  def inhabitants: List[Token] =
    members.toList flatMap {
      case t: Token => List(t)
      case g: Group => g.inhabitants
    }

}

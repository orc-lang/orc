//
// Group.scala -- Scala trait Group
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
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
  * Groups are not allowed to maintian references to values. This is required
  * because OrcSites have odd GC behavior and if a cycle through a group and
  * an OrcSite the OrcSiteCallTaget may never be collected. Token members of
  * the group can hold references to values, however it's important that when
  * the group halts or is killed there are no remaining references to members.
  *
  * @author dkitchin
  */
trait Group extends GroupMember {
  override def toString = super.toString + (if (alive) "" else "!!!") 

  override val nonblocking = true

  // These are of reduce visibility to make it harder for derived classes to violate the semantics of discorporation.
  // In general you should not mess with them. However they cannot be private because of debug code in Execution.
  private[core] val members: mutable.Buffer[GroupMember] = new mutable.ArrayBuffer(2)
  private var hasDiscorporatedMembers: Boolean = false

  val runtime: OrcRuntime

  /** Find the root of this group tree. */
  val root: Execution

  private var alive = true

  /** An expensive walk-to-root check for alive state */
  override def checkAlive(): Boolean = synchronized { alive }

  def publish(t: Token, v: Option[AnyRef]): Unit
  def onHalt(): Unit
  def onDiscorporate(): Unit
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
      // This is required to prevent persistent references to values from groups.
      members.clear()
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
        //println(s"Warning: adding $m to $this")
      } else {
        assert(!members.contains(m), s"Double Group.add of $m")
        members += m
      }
    }
    m match {
      case t: Token if (root.options.maxTokens > 0) => {
        if (root.tokenCount.incrementAndGet() > root.options.maxTokens)
          throw new TokenLimitReachedError(root.options.maxTokens)
      }
      case _ => {}
    }
  }

  private def maybeDecTokenCount(m: GroupMember) = m match {
    /* NOTE: We rely on the optimization that Tokens are not removed from their group when killed.
       * Thus, there is no kill-halt multiple remove issue for tokens.  */
    case t: Token if (root.options.maxTokens > 0) => root.tokenCount.decrementAndGet()
    case _ => {}
  }
  
  def remove(m: GroupMember) {
    synchronized {
      if (!alive) {
        //println(s"Warning: removing $m from $this")
      } else {
        assert(members contains m, s"Group $this does not contain $m")
        members -= m
        if (members.isEmpty) {
          if (hasDiscorporatedMembers)
            onDiscorporate()
          else {
            onHalt()
          }
        }
      }
    }
    maybeDecTokenCount(m)
  }

  def discorporate(m: GroupMember) {
    synchronized {
      members -= m
      hasDiscorporatedMembers = true
      if (members.isEmpty) { onDiscorporate() }
    }
    maybeDecTokenCount(m)
  }

  def inhabitants: List[Token] =
    members.toList flatMap {
      case t: Token => List(t)
      case g: Group => g.inhabitants
      case _ => Nil
    }

}

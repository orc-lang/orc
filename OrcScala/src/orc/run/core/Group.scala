//
// Group.scala -- Scala trait Group
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

import scala.collection.mutable

import orc.{ OrcExecutionOptions, OrcRuntime }
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
  private[run] val members: mutable.Set[GroupMember] = new mutable.HashSet;
  private var hasDiscorporatedMembers: Boolean = false

  /** Find the root of this group tree. */
  val execution: Execution
  def runtime: OrcRuntime = execution.runtime
  def options: OrcExecutionOptions = execution.options

  /* alive is volatile to allow reads to avoid locking.
   */
  @volatile
  private var alive = true

  /** An expensive walk-to-root check for alive state */
  override def checkAlive(): Boolean = synchronized { alive }

  def publish(t: Token, v: Option[AnyRef]): Unit
  def onHalt(): Unit
  def onDiscorporate(): Unit
  def run(): Unit

  def halt(t: Token) = remove(t)

  def kill() = {
    val mems = synchronized {
      if (alive) {
        alive = false
        val m = members.toArray[GroupMember]
        // This is required to prevent persistent references to values from groups.
        members.clear()
        m
      } else {
        Array[GroupMember]()
      }
    }
    for (m <- mems) {
      runtime.stage(m)
      /* Optimization: assume Tokens do not remove themselves from Groups */
      if (options.maxTokens > 0 && m.isInstanceOf[Token]) execution.tokenCount.decrementAndGet()
    }
  }

  /*
   * Note: This is a local live check. A global check requires a linear-time
   * ascension of the group tree.
   *
   * The live state of the token can change at any time. So this is only an
   * instantaneous test, meaning that the token may be killed immediately
   * after this function return false.
   */
  def isKilled() = !alive

  def suspend() = synchronized {
    for (m <- members) m.suspend()
  }

  def resume() = synchronized {
    for (m <- members) m.resume()
  }

  def add(m: GroupMember): Unit = {
    synchronized {
      if (!alive) {
        m.kill()
        //println(s"Warning: adding $m to $this")
      } else {
        /* This assert is useful, but since it's inside a hot-path synchronized block
         * it should be kept commented when not needed. On token intensive workloads
         * this and the other similarly marked assert can cost more than 10% on performance.
         */
        //assert(!members.contains(m), s"Double Group.add of $m")
        members += m
      }
    }
    m match {
      case t: Token if (options.maxTokens > 0) => {
        if (execution.tokenCount.incrementAndGet() > options.maxTokens)
          throw new TokenLimitReachedError(options.maxTokens)
      }
      case _ => {}
    }
  }

  private def maybeDecTokenCount(m: GroupMember) = m match {
    /* NOTE: We rely on the optimization that Tokens are not removed from their group when killed.
     * Thus, there is no kill-halt multiple remove issue for tokens.  */
    case t: Token if (options.maxTokens > 0) => execution.tokenCount.decrementAndGet()
    case _ => {}
  }

  /** Remove a member from this group.
    *
    * Returns true if the member is allowed to leave and false if the member cannot leave because it is already being killed.
    * If this returns, false then the member may be double scheduled.
    */
  def remove(m: GroupMember): Boolean = {
    val res = synchronized {
      if (!alive) {
        // Do not allow the member to escape if it has been killed.
        // At this point the token will have been double scheduled so it
        // cannot safely leave this group.
        false
      } else {
        /* This assert is useful, but since it's inside a hot-path synchronized block
         * it should be kept commented when not needed. On token intensive workloads
         * this and the other similarly marked assert can cost more than 10% on performance.
         */
        //assert(members contains m, s"Group $this does not contain $m")
        members -= m
        if (members.isEmpty) {
          if (hasDiscorporatedMembers) {
            runtime.stage(new GroupOnDiscorporate(this))
          } else {
            runtime.stage(new GroupOnHalt(this))
          }
        }
        true
      }
    }
    maybeDecTokenCount(m)
    res
  }

  def discorporate(m: GroupMember) {
    synchronized {
      if (!alive) {
        //println(s"Warning: removing $m from $this")
      } else {
        assert(members contains m, s"Group $this does not contain $m")
        members -= m
        hasDiscorporatedMembers = true
        if (members.isEmpty) {
          runtime.stage(new GroupOnDiscorporate(this))
        }
      }
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

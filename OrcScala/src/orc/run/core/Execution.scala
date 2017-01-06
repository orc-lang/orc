//
// Execution.scala -- Scala class Execution
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

import java.util.logging.Level

import orc.{ CaughtEvent, ExecutionRoot, HaltedOrKilledEvent, OrcEvent, OrcExecutionOptions, OrcRuntime, PublishedEvent }
import orc.ast.oil.nameless.Expression
import orc.error.runtime.TokenError
import orc.run.Logger
import orc.run.extensions.SupportForCallsIntoOrc

/** An execution is a special toplevel group,
  * associated with the entire program.
  *
  * @author dkitchin
  */
class Execution(
  val node: Expression,
  override val options: OrcExecutionOptions,
  private var eventHandler: OrcEvent => Unit,
  override val runtime: OrcRuntime)
  extends Group with ExecutionRoot with SupportForCallsIntoOrc {

  override val execution = this

  val tokenCount = new java.util.concurrent.atomic.AtomicInteger(0);

  //  def node = _node;

  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    notifyOrc(PublishedEvent(v.get))
    t.halt()
  }

  override def kill() = {
    /* Clean up the roots map when this is killed.
     * This cannot be an event handler because only one handler is allowed to
     * handle any event.
     * 
     * In the past roots contained weak references. However this combined with
     * the OrcSiteCallTargets resulted in entire group trees being collected all
     * at once without actually halting, which makes the interpreter hang 
     * without exiting. 
     */
    runtime.removeRoot(this)
    if (!isKilled) notifyOrc(HaltedOrKilledEvent)
    super.kill()
  }

  def onHalt() {
    // It's all the same at this level, so just deligate.
    onDiscorporate()
  }
  def onDiscorporate() {
    /* Clean up the roots map when this is killed.
     * This cannot be an event handler because only one handler is allowed to
     * handle any event.
     *
     * See kill for more details. 
     */
    runtime.removeRoot(this)
    if (!isKilled) notifyOrc(HaltedOrKilledEvent)
  }

  def run() = { throw new AssertionError("Execution scheduled") }

  val oldHandler = eventHandler
  eventHandler = {
    case e @ CaughtEvent(je: Error) => { Logger.log(Level.SEVERE, "Unexpected Java Error thrown; killing Orc Execution", je); oldHandler(e); kill() }
    case e @ CaughtEvent(_: TokenError) => { oldHandler(e); kill() }
    case e => oldHandler(e)
  }

  def installHandler(newHandler: PartialFunction[OrcEvent, Unit]) = {
    val oldHandler = eventHandler
    eventHandler = { e => if (newHandler isDefinedAt e) newHandler(e) else oldHandler(e) }
  }

  def notifyOrc(event: OrcEvent) {
    try {
      if (event == DumpState) dumpState()
      eventHandler(event)
    } catch {
      case e: InterruptedException => throw e
      case e: Throwable => { Logger.log(Level.SEVERE, "Event handler abnormal termination", e); throw e }
    }
  }

  def dumpState() {
    def printGroupMember(currMember: GroupMember, level: Int, sb: StringBuilder) {
      for { i <- 1 until level * 2 } sb.append(' ')
      currMember match {
        case t: Token => {
          /* Already dumped Token state, just print name/hashcode */
          sb.append(t.getClass.getName)
          sb.append('@')
          sb.append(Integer.toHexString(t.##))
          sb.append('\n')
        }
        case g: Group => {
          sb.append(g.toString)
          sb.append('\n')
          g.members map { printGroupMember(_, level + 1, sb) }
        }
        case null => {
          // TODO: Figure out how we ever end up getting null here.
          sb.append("null")
          sb.append('\n')
        }
        case _ => {
          sb.append(currMember.toString)
          sb.append('\n')
        }
      }
    }
    val sb = new StringBuilder()
    sb.append("Orc execution state dump at ")
    sb.append(String.format("%1$TF %1$TT.%1$TL %1$TZ", java.lang.Long.valueOf(System.currentTimeMillis())))
    sb.append("\n\nToken states:\n")
    inhabitants map { m =>
      sb.append(m)
      sb.append(" at ")
      sb.append(m.sourcePosition.getOrElse("<unknown location>"))
      if (m.sourcePosition.isDefined) {
        sb.append(":\n")
        sb.append(m.sourcePosition.get.lineContentWithCaret)
      } else {
        sb.append("\n")
      }
      val callPoints = m.getStack.toList collect { case f: FunctionFrame => f.callpoint.sourceTextRange }
      sb.append('\n')
      callPoints map { p => sb.append("\tcalled at " + p.getOrElse("<unknown location>") + "\n") }
      sb.append('\n')
    }
    sb.append("Group tree:\n")
    printGroupMember(this, 1, sb)
    sb.append('\n')
    val prefix = "# "
    Console.err.println(prefix + sb.toString.stripLineEnd.replaceAll("\n", "\n" + prefix))
  }
}

object DumpState extends OrcEvent

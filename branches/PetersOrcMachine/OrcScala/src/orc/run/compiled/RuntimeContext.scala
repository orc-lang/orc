//
// RuntimeContext.scala -- Scala class/trait/object RuntimeContext
// Project OrcScala
//
// $Id$
//
// Created by amp on May 21, 2014.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.compiled

import orc.run.StandardInvocationBehavior
import java.util.concurrent.LinkedBlockingDeque
import orc.Handle
import orc.OrcEvent
import orc.run.extensions.RwaitEvent
import orc.values.sites.HaltedException
import orc.error.OrcException
import orc.CaughtEvent
import orc.run.compiled.OrcModuleInstance
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger
import orc.run.porc.KilledException
import orc.run.porc.TerminableHandler
import scala.util.parsing.input.NoPosition

/** @author amp
  */
trait RuntimeContext extends StandardInvocationBehavior {
  type Action = () => Unit

  @inline
  def schedule(p: Action, h: Action): Unit = {
    schedule(() => {
      try {
        p()
      } catch {
        case _: KilledException => ()
      }
      try {
        h()
      } catch {
        case _: KilledException => ()
      }
    })
  }
  def schedule(f: Action): Unit = {
    queue.add(f)
  }
  def dequeue(): Option[Action] = {
    val t = queue.poll()
    Option(t)
  }
  def steal(): Option[Action] = {
    val t = queue.pollLast()
    Option(t)
  }

  val runtime: Runtime

  // TODO: This needs to be a special dequeue implementation.
  val queue = new LinkedBlockingDeque[Action]()
}

final class RuntimeThread(val runtime: Runtime) extends Thread with RuntimeContext {
  private[compiled] var otherContexts: Seq[RuntimeContext] = Nil

  @volatile
  private var running = true

  override def run() {
    while (running) {
      dequeue() match {
        case Some(f) =>
          f()
        case None =>
          // steal work
          // TODO: Steal more than one item to amortize the cost
          otherContexts.find(c => !c.queue.isEmpty()) flatMap { _.steal } match {
            case Some(t) => {
              //println("Stealing work.")
              schedule(t)
            }
            case None => {
              // Wait a little while and see if more work has appeared
              runtime.waitForWork()
            }
          }
      }
    }
  }

  def kill() = {
    running = false
  }
}

final class CallHandle(pc: (AnyRef) => Unit, hc: () => Unit,
  terminator: Terminator, moduleInstance: OrcModuleInstance) extends Handle with Terminable {
  final protected def ctx: RuntimeContext = {
    Thread.currentThread() match {
      case ctx: RuntimeContext => ctx
      case _ => moduleInstance.defaultContext
    }
  }

  def kill() = synchronized {
    if (terminator != null) {
      ctx.schedule(hc)
      clear()
    }
  }

  def callSitePosition: scala.util.parsing.input.Position = NoPosition
  def setQuiescent(): Unit = {} // Ignore as we don't support virtual time

  def notifyOrc(event: OrcEvent): Unit = synchronized {
    event match {
      // TODO: There are problably other events I need to handle
      case RwaitEvent(d, h) => ctx.runtime.registerDelayed(d.toLong, () => h.publish())
      case _ => moduleInstance.eventHandler(event)
      //Logger.severe(event.toString)
    }
  }

  // FIXME: It would be much better to be able to make the call directly here. However 
  // that would require differentiating calls in the same thread and those in other threads. 
  // AND it would require a counter to prevent the stack from overflowing in deep recursion.
  def publish(v: AnyRef): Unit = synchronized {
    if (!isLive) return ;

    if (terminator.removeTerminable(this)) {
      Logger.finer(s"Site call published: -> $v   $this")
      /*if(startingThread == Thread.currentThread) {
          result = CallValue(v)
        } else*/
      ctx.schedule(() => pc(v), hc)
    }
    clear()
  }
  def halt: Unit = synchronized {
    if (!isLive) return ;

    if (terminator.removeTerminable(this)) {
      Logger.finer(s"Site call halted. $this")
      /*if(startingThread == Thread.currentThread) {
          result = CallHalt
        } else*/
      ctx.schedule(hc)
    }
    clear()
  }

  def !!(e: OrcException): Unit = synchronized {
    if (!isLive) return ;

    notifyOrc(CaughtEvent(e))
    halt
  }

  def hasRight(rightName: String): Boolean = {
    moduleInstance.options.hasRight(rightName)
  }

  def isLive: Boolean = {
    terminator != null && !terminator.isKilled
  }

  private def clear(): Unit = {
    /*
    terminator = null
    interp = null
    hc = null
    pc = null
    */
  }
}

final class Terminator extends TerminableHandler {
  // Fields are public to allow inlining by the scala compiler, however they should not be directly accessed. 

  /** The collection of kill handlers. Null means this terminator has been killed.
    */
  var killHandlers = new ArrayList[() => Unit]()

  /** Similar to _killHandlers for terminables. They are called directly
    * (with the terminator lock held, instead of being handled in callKillHandlers).
    */
  var terminables = new ArrayList[Terminable]()

  Logger.fine(s"created $this")

  /** Is this terminator in the killed state?
    */
  def isKilled = synchronized {
    killHandlers == null
  }

  /** Kill this terminator and return the kill handlers if this is the first time it has been killed.
    */
  def setIsKilled(): Option[Seq[() => Unit]] = {
    import collection.JavaConversions._

    val r = this.synchronized {
      val khs = killHandlers
      if (khs != null) {
        killHandlers = null

        Some(khs.toSeq)
      } else {
        None
      }
    }

    if (r.isDefined) {
      // FIXME: Does this actually need the defensive copy?
      for (t <- this.synchronized { Seq() ++ terminables }) {
        t.kill()
      }

      terminables = null
    }

    r
  }

  /** Add a kill handler if this is not killed. Return true if the handler was added,
    * false if this is already killed.
    */
  def addKillHandler(kh: () => Unit) = synchronized {
    if (isKilled) {
      false
    } else {
      killHandlers.add(kh)
      true
    }
  }

  /** Add a terminable to the list that will be called on kill. If the terminator
    * will be called return true. If not, return false. This second case occures
    * if this terminator is already killed.
    */
  def addTerminable(t: Terminable): Boolean = synchronized {
    if (!isKilled) {
      terminables.add(t)
      true
    } else {
      false
    }
  }
  /** Remove a terminable and return true if it was there before. This will return
    * false if the terminator has already COMPLETED it's kill on this terminable.
    * To simplify logic in the terminables during the kill processing this will
    * still return true.
    */
  def removeTerminable(t: Terminable): Boolean = synchronized {
    if (terminables != null)
      terminables.remove(t) // Returns true if the element was present in the list
    else
      false
  }
}

final class Counter {
  private[compiled] val count: AtomicInteger = new AtomicInteger(1)

  Logger.fine(s"created $this")

  def incrementOrKill() {
    if (!incrementInt()) {
      throw KilledException
    }
  }

  def increment() {
    if (!incrementInt()) {
      throw new AssertionError("Incrementing zero counter")
    }
  }

  /** Increment the counter if it is positive and return true. If
    * it is zero return false and do not increment.
    */
  private def incrementInt() = {
    val v = count.get()
    if (v <= 0) {
      Logger.fine(s"Incr on dead Counter $this ($v)")
      false
    } else {
      Logger.finer(s"incr $this ($v)") //:\n${StackUtils.getStack()}
      count.incrementAndGet()
      true
    }
  }

  /** Decrement the counter and return true if it reached zero.
    */
  def decrementAndTestZero() = {
    val v = count.get()
    if (v <= 0) {
      Logger.info(s"Uberkilling old Counter $this ($v)")
      assert(false)
      false
    } else {
      Logger.finer(s"decr $this (${v - 1})") //:\n${StackUtils.getStack()}
      if (count.decrementAndGet() == 0) {
        synchronized {
          notifyAll()
        }
        true
      } else {
        false
      }
    }
  }

  @volatile
  var haltHandler: () => Unit = null
  /*
  def haltHandler = synchronized {
    assert(_haltHandler != null)
    Logger.finest(s"Getting halt $this, ${_haltHandler}")
    _haltHandler
  }
  def haltHandler_=(v: Closure) = synchronized { 
    assert(_haltHandler == null)
    Logger.finest(s"Setting halt $this, $v")
    _haltHandler = v
  }
  */

  def waitZero() {
    synchronized {
      while (count.get() != 0) wait()
    }
  }
}

//
// OrcModule.scala -- Scala class/trait/object OrcModule
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

import orc.OrcEvent
import orc.OrcExecutionOptions

/**
  *
  * @author amp
  */

abstract class OrcModuleInstance(
    protected val initCounter: Counter, 
    val eventHandler: (OrcEvent) => Unit,
    val options: OrcExecutionOptions,
    val defaultContext: RuntimeContext) {
  def apply(): Unit
  
  @inline
  final protected def spawn(f: () => Unit, counter: Counter) {
    counter.incrementOrKill()
    ctx.schedule(f, counter.haltHandler)
  }
  @inline
  final protected def spawn(f: (Seq[AnyRef]) => Unit, counter: Counter) {
    counter.incrementOrKill()
    ctx.schedule(() => f(Nil), counter.haltHandler)
  }

  final protected def invokeExternal(callable: AnyRef, arguments: Seq[AnyRef], pc: AnyRef => Unit, counter: Counter, terminator: Terminator): Unit = {
    val hc = counter.haltHandler
    counter.incrementOrKill() // We are spawning a process effectively

    val handle = new CallHandle(pc, hc, terminator, this)
    Logger.finer(s"Site call started: $callable $arguments   $handle")

    if (terminator.addTerminable(handle)) {
      ctx.invoke(handle, callable, arguments.toList) 
      // TODO: This could probably be more performant by threading the list through. However really this should probably be an array.
    } else {
      Logger.finer(s"Site call killed immediately. $this")
      handle.kill()
    }
  }

  final protected def ctx: RuntimeContext = {
    Thread.currentThread() match {
      case ctx: RuntimeContext => ctx
      case _ => defaultContext
    }
  }

  final protected def forceFutures(vs: Seq[AnyRef], bb: (Seq[AnyRef] => Unit), counter: Counter, terminator: Terminator): Unit = {
    val fs = vs.zipWithIndex.collect { case (f: Future, i) => (i, f) }.toMap
    val hb = counter.haltHandler

    if (fs.isEmpty) {
      // TODO: Optimize to only trampoline when the stack is tall.
      //Call(bb, vs).eval(ctx, interp)
      counter.increment() // We are now spawning something that will call the halt handler
      ctx.schedule(() => bb(vs), hb)
    } else {
      counter.increment() // We are now spawning something that will call the halt handler
      val j = new Join(fs, terminator) {
        def halt() {
          Logger.finer(s"Future halted: calling $hb")
          ctx.schedule(hb)
        }
        def bound(nvs: Map[Int, AnyRef]) {
          val args = vs.zipWithIndex.map { p =>
            nvs.getOrElse(p._2, p._1)
          }
          Logger.finer(s"Future bound: calling $bb ($args)")
          ctx.schedule(() => bb(args), hb)
        }
      }
      Logger.finer(s"force started: $j")
      if (terminator.addTerminable(j)) {
        for ((_, f) <- fs) {
          f.addBlocked(j)
        }
      } else {
        Logger.finer(s"Future killed immediately: calling $hb")
        ctx.schedule(hb)
      }
    }
  }

  final protected def resolveFuture(v: AnyRef, bb: () => Unit, counter: Counter, terminator: Terminator): Unit = {
    val hb = counter.haltHandler

    v match {
      case f: Future =>
        counter.increment() // We are now spawning something that will call the halt handler
        val j = new Join(Map(0 -> f), terminator) {
          def halt() {
            Logger.finer(s"Future resolved: calling $bb")
            ctx.schedule(bb, hb)
          }
          def bound(nvs: Map[Int, AnyRef]) {
            halt()
          }
        }
        Logger.finer(s"resolve started: $j")
        if (terminator.addTerminable(j)) {
          f.addBlocked(j)
        } else {
          Logger.finer(s"Future killed immediately.")
          j.halt()
        }
      case _ =>
        // TODO: Optimize to only trampoline when the stack is tall.
        // bb()
        counter.increment() // We are now spawning something that will call the halt handler
        ctx.schedule(bb, hb)
    }
  }
}
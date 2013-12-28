//
// Interpreter.scala -- Scala class/trait/object Interpreter
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 15, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porc

import java.util.concurrent.atomic.AtomicInteger
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import orc.OrcEvent
import orc.OrcExecutionOptions
import java.util.concurrent.ConcurrentLinkedQueue

case class ResilientCounterEvent(c: Counter) extends OrcEvent

/**
  *
  * @author amp
  */
class Interpreter {
  class ExecutionHandle(initialElements: Counter*) {
    import scala.collection.JavaConversions._
    
    // FIXME: This holds references to counters for an unbounded amount of time. It might be better to allow them to remove themselves when they zero. This could even be converted to a counter in that case. 
    val counters = new ConcurrentLinkedQueue[Counter]
    counters.addAll(initialElements)
    
    def waitForHalt() {
      while(!counters.isEmpty()) {
        val c = counters.poll()
        Logger.fine(s"Waiting on counter: $c")
        c.waitZero()
        Logger.fine(s"Counter zeroed: $c")
      }
    }
    
    def addCounter(c: Counter) {
      Logger.fine(s"Adding counter: $c")
      counters.offer(c)
    }
  }
  
  private val runningCount: AtomicInteger = new AtomicInteger(1)
  
  private val processors = Runtime.getRuntime().availableProcessors()
  private var threads: IndexedSeq[InterpreterThread] = IndexedSeq()
  
  var debugTable = PorcDebugTable()

  // FIXME: Blocked threads are not replaced. I need a facility that marks blocked threads and spawns new ones. 
  // spawn 4 threads per core (currently do not worry about blocked threads)
  val nthreads = if(false) 1 else processors * 4
  
  threads = for(_ <- 1 to nthreads) yield {
    new InterpreterThread(this)
  }
  runningCount.set(threads.size)

  // Tell them all about the other contexts and start them all.
  val ctxs = threads.map(_.ctx).toList
  for(t <- threads) {
    t.otherContexts = ctxs
    t.start()
  }

  //Logger.logAllToStderr()
  
  def start(e: Expr, k: OrcEvent => Unit, options: OrcExecutionOptions, exprDebugTable: PorcDebugTable = PorcDebugTable()) : ExecutionHandle = {
    // Insert the initial program state into one of the threads.
    val initCounter = new Counter()
    
    val executionHandle = new ExecutionHandle(initCounter)
    def eventHandler(e: OrcEvent) = e match {
      case ResilientCounterEvent(c) =>
        executionHandle.addCounter(c)
      case _ =>
        k(e)
    }
    
    debugTable ++= exprDebugTable
    
    threads(0).ctx.schedule(Closure(e, Context(Nil, new Terminator(), initCounter, null, eventHandler, options)), Nil)
    
    executionHandle
  }
  
  def kill() {
    for(t <- threads) {
      t.kill()
    }
    
    timer.cancel()
  }
  
  def waitForWork() {
    if( runningCount.decrementAndGet() == 0 ) {
      // We are done, All threads are waiting for work.
      //kill()
    } else {
      Thread.sleep(0, 100)
      runningCount.incrementAndGet()
    }
  }
  
  val timer: Timer = new Timer()

  def registerDelayed(delay: Long, f: () => Unit) = {
    val callback =
      new TimerTask() {
        @Override
        override def run() { f() }
      }
    timer.schedule(callback, delay)
  }
}

final class InterpreterThread(val interp: Interpreter) extends Thread {
  setDaemon(true)
  
  val ctx = new InterpreterContext(interp)
  var otherContexts = List[InterpreterContext]()
 
  @volatile
  private var running = true
  
  override def run() {
    InterpreterContext current_= ctx
    
    while(running) {
      ctx.dequeue() match {
        case Some(ScheduleItem(clos, args, halt, trace)) =>
          val ectx = clos.ctx.pushValues(args).setTrace(trace)
          Logger.fine(s"Executing $clos $args")
          try {
            clos.body.eval(ectx, ctx)
          } catch {
            case _ : KilledException => ()
          }
          if( halt != null ) {
            Logger.fine(s"Executing halt $halt")
            try {
              halt.body.eval(halt.ctx.setTrace(trace), ctx)
            } catch {
              case _ : KilledException => ()
            }
          }
        case None => 
          // steal work
          otherContexts.find(c => !c.queue.isEmpty()) flatMap { _.steal } match { 
            case Some(t) => {
              //println("Stealing work.")
              ctx.schedule(t)
            }
            case None => {
              // Wait a little while and see if more work has appeared
              interp.waitForWork()
            }
          }
      }
    }
  }
  
  def kill() = {
    running = false
  }
}
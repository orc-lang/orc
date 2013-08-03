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

/**
  *
  * @author amp
  */
class Interpreter {
  private val processors = Runtime.getRuntime().availableProcessors()
  private var threads: IndexedSeq[InterpreterThread] = IndexedSeq()
  
  //Logger.logAllToStderr()
  
  def start(e: Expr) {
    // spawn 2 threads per core (currently do not worry about blocked threads)
    val nthreads = if(false) 1 else processors * 2
    threads = for(_ <- 1 to nthreads) yield {
      new InterpreterThread(this)
    }
    runningCount.set(threads.size)
    // Insert the initial program state into one of the threads.
    val initCounter = new Counter()
    InterpreterContext.default = threads(0).ctx
    
    threads(0).ctx.schedule(Closure(e, Context(Nil, new Terminator(), initCounter, null)), Nil)
    // Tell them all about the other contexts and start them all.
    val ctxs = threads.map(_.ctx).toList
    for(t <- threads) {
      t.otherContexts = ctxs
      t.start()
    }
  }
  
  def kill() {
    for(t <- threads) {
      t.kill()
    }
    
    timer.cancel()
  }

  private val runningCount: AtomicInteger = new AtomicInteger(1)
  
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
  val ctx = new InterpreterContext(interp)
  var otherContexts = List[InterpreterContext]()
 
  @volatile
  private var running = true
  
  override def run() {
    InterpreterContext.current = ctx
    
    while(running) {
      ctx.dequeue() match {
        case Some((clos, args, halt)) =>
          val ectx = clos.ctx.pushValues(args)
          Logger.fine(s"Executing $clos $args")
          try {
            clos.body.eval(ectx, ctx)
          } catch {
            case _ : KilledException => ()
          }
          Logger.fine(s"Executing halt $halt")
          if( halt != null ) {
            try {
              halt.body.eval(halt.ctx, ctx)
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
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

/**
  *
  * @author amp
  */
class Interpreter {
  private val processors = Runtime.getRuntime().availableProcessors()
  private var threads: IndexedSeq[InterpreterThread] = IndexedSeq()
  
  //Logger.logAllToStderr()
  
  def start(e: Command) {
    // spawn 2 threads per core (currently do not worry about blocked threads)
    val nthreads = processors * 2
    threads = for(_ <- 1 to nthreads) yield {
      new InterpreterThread()
    }
    // Insert the initial program state into one of the threads.
    val initCounter = new Counter()
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
  }
}

final class InterpreterThread extends Thread {
  val ctx = new InterpreterContext()
  var otherContexts = List[InterpreterContext]()
 
  @volatile
  private var running = true
  
  override def run() {
    InterpreterContext.current = ctx
    
    while(running) {
      ctx.dequeue() match {
        case Some((clos, args)) =>
          val ectx = clos.ctx.pushValues(args)
          //println((clos, args))
          clos.body.eval(ectx, ctx)
        case None => 
          // steal work
          // FIXME: This busy waits. I need a better underlying data structure.
          Thread.sleep(10)
          otherContexts.find(c => !c.queue.isEmpty()) flatMap { _.dequeue } foreach { t =>
            ctx.schedule(t._1, t._2)
          }
          // stealing disabled for now. Just exit.
          //running = false
      }
    }
  }
  
  def kill() = {
    running = false
  }
}
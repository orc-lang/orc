//
// ManyActorBasedScheduler.scala -- Scala trait ManyActorBasedScheduler
// Project OrcScala
//
// $Id$
//
// Created by amshali on Aug 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import scala.actors.Actor
import scala.actors.IScheduler
import scala.collection.mutable.Queue
import scala.collection.JavaConversions._ 


/**
 * Maintains a collection of worker threads, and
 * schedules work on them in a round-robin fashion.
 * If work arrives when no thread is available, a
 * new thread is started.
 *
 * @author amshali, dkitchin
 */
trait ManyActorBasedScheduler extends Orc {

  private val myScheduler = new scala.actors.scheduler.ResizableThreadPoolScheduler(true, false)
  myScheduler.start()

  // distribute the work between actors in a round-robin fashion
  override def schedule(ts: List[Token]) {
    for (t <- ts) {
      ActorPool.get() ! Some(t)
    }
  }

  object ActorPool {

    val idleWorkers: java.util.Queue[Worker] = new java.util.concurrent.ConcurrentLinkedQueue()

    val allWorkers: java.util.LinkedList[Worker] = new java.util.LinkedList() 

    def get(): Worker = synchronized {
      if (idleWorkers.isEmpty) {
        val newWorker = new Worker().start()
        allWorkers.add(newWorker)
        newWorker
      } else {
        idleWorkers.poll()
      }
    }

    def store(w: Worker) {
      idleWorkers.offer(w)
    }

    def shutdown() {
      for (currWorker <- allWorkers) {
        currWorker ! None
      }
    }

  }
  
  /**
   * Shut down this runtime and all of its backing threads.
   * All executions stop without cleanup, though they are not guaranteed to stop immediately. 
   * This will cause all synchronous executions to hang. 
   */
  // TODO: Implement cleaner alternatives.
  override def stop = {
    ActorPool.shutdown()
    super.stop
  }

  class Worker extends Actor {

    override def start() = {
      super.start()
      this
    }

    def act() {
      loop {
        react {
          case Some(x: Token) => {
            // If this thread should be interrupted, then reflect now, rather than wait for I/O
            if (Thread.interrupted()) // Note: Clears thread's interrupted status bit
              throw new InterruptedException()
            x.run()
            ActorPool.store(this)
          }
          // machine has stopped
          case None => exit
        }
      }
    }

    override def scheduler: IScheduler = myScheduler

  }

}

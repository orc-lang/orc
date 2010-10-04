//
// CappedActorBasedScheduler.scala -- Scala class/trait/object CappedActorBasedScheduler
// Project OrcScala
//
// $Id$
//
// Created by amshali on Oct 4, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import scala.concurrent._
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.Queue

/**
 * 
 *
 * @author amshali
 */
trait CappedActorBasedScheduler extends Orc with SupportForVtimer {
  scala.actors.Scheduler.impl.shutdown
  scala.actors.Scheduler.impl = {
    val s = new scala.actors.scheduler.ResizableThreadPoolScheduler(true, false)
    s.start()
    s
  }

  // distribute the work between actors in a round-robin fashion
  override def schedule(ts: List[Token]) {
    tokensRunning.addAndGet(ts.size)
    for (t <- ts) {
      ActorPool.assign(Some(t))
    }
  }
  val CAP = 256
  
  override def scheduleK(k : K) {
    kCount.incrementAndGet()
    ActorPool.assign(k)
  }
  val works : java.util.Queue[AnyRef] = new java.util.concurrent.ConcurrentLinkedQueue()

  object ActorPool {
    val queue : java.util.Queue[Worker] = new java.util.concurrent.ConcurrentLinkedQueue()
    val all : java.util.LinkedList[Worker] = new java.util.LinkedList() 
    
    def assign(wrk : AnyRef) = synchronized {
      if (all.size < CAP) {
        val worker = (
          if (queue.size == 0) {
            val tmp = new Worker().start
            all.add(tmp)
            tmp
          }
          else queue.poll)
        worker ! wrk
      }
      else {
        works.offer(wrk)
      }
    }
    

    def store(w : Worker) {
      queue.offer(w)
    }
    def shutdown() {
      var i = 0
      while (i < all.size) {
        all.get(i) ! None
        i = i + 1
      }
    }
  }
  
  /* Shut down this runtime and all of its backing threads.
   * All executions stop without cleanup, though they are not guaranteed to stop immediately. 
   * This will cause all synchronous executions to hang. 
   */
  // TODO: Implement cleaner alternatives.
  override def stop = {
    ActorPool.shutdown
    super.stop
  }

  class Worker extends Actor {
    override def start() = {
      super.start
      this
    }
    
    def steal(a : Any) {
      var w = a
      while(w != null) {
        w match {
          case Some(x: Token) => {
            // If this thread should be interrupted, then reflect now, rather than wait for I/O
            if (Thread.interrupted()) // Note: Clears thread's interrupted status bit
              throw new InterruptedException()
            x.run
            decTokensRunning()
          }
          case K(k, ov) => {
            k(ov)
            deckCount()
          }
        }
        w = works.poll()
      }
    }
    def act() {
      loop {
        react {          
          // machine has stopped
          case None => exit
          case a => steal(a); ActorPool.store(this)
        }
      }
    }
  }

}

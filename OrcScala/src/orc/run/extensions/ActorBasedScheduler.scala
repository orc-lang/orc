//
// ActorBasedScheduler.scala -- Scala trait ActorBasedScheduler
// Project OrcScala
//
// $Id: ActorBasedScheduler.scala 2228 2010-12-07 19:13:50Z jthywissen $
//
// Created by dkitchin on Jul 10, 2010.
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


/**
 * 
 *
 * @author amshali, dkitchin
 */
import scala.actors.Actor
import scala.actors.Actor._
  
trait ActorBasedScheduler extends Orc {
  val workers : List[Worker] = 
    List(new Worker(), new Worker(), new Worker(), new Worker())
          
  for (w <- workers) {w.start}
  
  // distribute the work between actors in a round-robin fashion
  override def schedule(ts: List[Token]) = synchronized { 
    var i = 0
    for (t <- ts) {
      workers(i) ! Some(t)
      i = (i + 1) % workers.size
    }
  }
  
  /* Shut down this runtime and all of its backing threads.
   * All executions stop without cleanup, though they are not guaranteed to stop immediately. 
   * This will cause all synchronous executions to hang. 
   */
  // TODO: Implement cleaner alternatives.
  override def stop = {
    for (w <- workers) w ! None
    super.stop 
  }
  
  class Worker extends Actor {
    def act() {
      loop {
        react {
          case Some(x:Token) => {
            // If this thread should be interrupted, then reflect now, rather than wait for I/O
            if (Thread.interrupted())  // Note: Clears thread's interrupted status bit
              throw new InterruptedException()
            x.run
          }
          // machine has stopped
          case None => exit
        }
      }
    }
  }
  
}

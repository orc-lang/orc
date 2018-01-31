//
// PrioritySchedulingQueue.scala -- 
// Project OrcScala
//
// Created by amp on Jan, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import orc.Schedulable
import java.util.concurrent.PriorityBlockingQueue

class PrioritySchedulingQueue extends SchedulingQueue[Schedulable] {
  @volatile
  private[this] var offset = 0
  
  private case class SchedulableWrapper(schedulable: Schedulable) extends Comparable[SchedulableWrapper] {
    val pri = offset + schedulable.priority
    def compareTo(o: SchedulableWrapper): Int = pri compareTo o.pri 
  }
  
  private[this] val q = new PriorityBlockingQueue[SchedulableWrapper]
  
  def push(o: Schedulable): Boolean = {
    q.offer(SchedulableWrapper(o))
  }
  
  def pop(): Schedulable = {
    val p = q.poll()
    if (p == null)
      null
    else {
      offset += 1
      p.schedulable
    }
  }
  def steal(): Schedulable = pop()
  
  def size: Int = {
    q.size
  }
  
  def clean(): Unit = {
    offset = 0
    q.clear()
  }
}
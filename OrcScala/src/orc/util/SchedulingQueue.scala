//
// SchedulingQueue.scala -- A queue interface for work-stealing queues
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

trait SchedulingQueue[T] {
  /** Push an item into the queue.
    *
    * Only call from the owner thread.
    *
    * @returns true if the insert was successful, false if it failed due to a full queue.
    */
  def push(o: T): Boolean

  /** Pop an item from the queue.
    *
    * Only call from the owner thread.
    *
    * @returns null if the queue is empty.
    */
  def pop(): T

  /** Pop an item from the queue.
    *
    * This traditionally pops from the bottom of the queue (work-stealing style).
    *
    * May be called from non-owner threads.
    *
    * @returns null if the queue is empty.
    */
  def steal(): T

  /** Get the size of the queue
    *
    * This is only an estimate and the queue may never have had the returned length. The
    * actually length must be less than (or equal to) this number and may only decrease 
    * unless the owner adds items.
    * 
    * This may only be called from the owner thread.
    */
  def size: Int

  /** Clear the backing storage of the queue.
    *
	  * This is useful to allow old items in the queue to be freed.
    *
    * This may only be called from the owner thread when the queue is empty.
    */
  def clean(): Unit
}
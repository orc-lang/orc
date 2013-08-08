//
// SynchronousThreadExec.scala -- Scala object SynchronousThreadExec
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 23, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

import scala.util.{Try, Success, Failure}
import java.util.concurrent.TimeoutException

/** SynchronousThreadExec runs a thunk (block expression) in a new
  * thread, and waits for the result: either a returned value or a
  * throwable.  This is useful when a block of code needs a new
  * JVM thread context, but doesn't need any concurrency with the
  * caller.
  *
  * @author jthywiss
  */
object SynchronousThreadExec {

  def apply[T](threadName: String, thunk: => T): T = apply(threadName, 0, thunk)

  def apply[T](threadName: String, maxWaitMillis: Long, thunk: => T): T = {
    var returnVal: Try[T] = null
    val thunkWrapper = new Runnable {
      def run() {
        try {
          returnVal = Success(thunk)
        } catch {
          case t: Throwable => returnVal = Failure(t)
        }
      }
    }
    val thunkThread = new Thread(thunkWrapper, threadName)
    thunkThread.start()
    thunkThread.join(maxWaitMillis)
    if (thunkThread.isAlive()) {
      thunkThread.interrupt()
      /* "thunkThread, you have 1 ms to get your affairs in order..." */
      thunkThread.join(1L)
      throw new TimeoutException()
    }
    returnVal.get
  }

}

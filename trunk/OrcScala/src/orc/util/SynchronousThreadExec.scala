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

/** SynchronousThreadExec runs a thunk (block expression) in a new
  * thread, and waits for the result: either a returned value or a
  * throwable.  This is useful when a block of code needs a new
  * JVM thread context, but doesn't need any concurrency with the
  * caller.
  *
  * @author jthywiss
  */
object SynchronousThreadExec {

  def apply[T](threadName: String, thunk: => T): T = {
    var returnVal: ReturnOrThow[T] = null
    val thunkWrapper = new Runnable {
      def run() {
        try {
          returnVal = Return(thunk)
        } catch {
          case t: Throwable => returnVal = Throw(t)
        }
      }
    }
    val thunkThread = new Thread(thunkWrapper, threadName)
    thunkThread.start()
    thunkThread.join()
    returnVal.get
  }

}


sealed abstract class ReturnOrThow[+A] {
  def get: A
}

final case class Return[+A](x: A) extends ReturnOrThow[A] {
  def get = x
}

final case class Throw(t: Throwable) extends ReturnOrThow[Nothing] {
  def get = throw t
}

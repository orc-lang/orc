//
// StackTrace.scala -- Scala object and class StackTrace
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import sun.misc.SharedSecrets

class StackTrace(val frames: Array[StackTraceElement]) extends scala.collection.immutable.IndexedSeq[StackTraceElement] {
  def length = frames.length
  def apply(i: Int) = frames(i)
  override def toString() = frames.mkString("\n")
}

object StackTrace {
  def getStackTrace(skip: Int = 0, n: Int = Int.MaxValue) = {
    val e = new Exception();
    val depth = Math.min(n, SharedSecrets.getJavaLangAccess().getStackTraceDepth(e));
    val result = new Array[StackTraceElement](depth)
    val offset = 1 + skip

    for (frame <- 0 until depth) {
      result(frame) = SharedSecrets.getJavaLangAccess().getStackTraceElement(e, frame + offset);
    }

    new StackTrace(result)
  }
}

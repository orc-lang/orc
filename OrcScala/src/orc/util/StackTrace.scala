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

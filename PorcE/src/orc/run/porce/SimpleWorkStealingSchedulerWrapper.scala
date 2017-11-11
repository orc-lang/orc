package orc.run.porce

import orc.run.extensions.{ SimpleWorkStealingScheduler }
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import orc.Schedulable

object SimpleWorkStealingSchedulerWrapper {
  import SimpleWorkStealingScheduler.traceTasks
  
  @TruffleBoundary @noinline
  def newSchedulableID() = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.newSchedulableID()
    } else {
      0
    }
  }
  
  @TruffleBoundary @noinline
  def getSchedulableID(s: Schedulable) = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.getSchedulableID(s)
    } else {
      0
    }
  }
  
  @TruffleBoundary @noinline
  def traceTaskParent(parent: Schedulable, child: Schedulable): Unit = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }
  }
  
  @TruffleBoundary @noinline
  def traceTaskParent(parent: Schedulable, child: Long): Unit = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }
  }
  
  @TruffleBoundary @noinline
  def traceTaskParent(parent: Long, child: Long): Unit = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }
  }  
  
  @TruffleBoundary @noinline
  def enterSchedulable(s: Schedulable, t: SimpleWorkStealingScheduler.SchedulableExecutionType): Unit = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.enterSchedulable(s, t)
    }
  }
  
  @TruffleBoundary @noinline
  def currentSchedulable: Schedulable = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.currentSchedulable
    } else {
      null
    }
  }
  
  @TruffleBoundary @noinline
  def exitSchedulable(s: Schedulable): Unit = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.exitSchedulable(s)
    }
  }
  
  @TruffleBoundary @noinline
  def exitSchedulable(s: Schedulable, old: Schedulable): Unit = {
    if (traceTasks) {
      SimpleWorkStealingScheduler.exitSchedulable(s, old)
    }
  }
}
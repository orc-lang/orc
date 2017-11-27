package orc.run.porce

import orc.run.extensions.{ SimpleWorkStealingScheduler }
import com.oracle.truffle.api.CompilerDirectives.{ TruffleBoundary, CompilationFinal }
import orc.Schedulable

object SimpleWorkStealingSchedulerWrapper {
  @CompilationFinal
  private val traceTasks = SimpleWorkStealingScheduler.traceTasks

  def shareSchedulableID(d: AnyRef, s: AnyRef): Unit = {
    if (traceTasks) {
      Boundaries.shareSchedulableID(d, s)
    }
  }

  def getSchedulableID(s: AnyRef) = {
    if (traceTasks) {
      Boundaries.getSchedulableID(s)
    } else {
      0
    }
  }

  def traceTaskParent(parent: AnyRef, child: AnyRef): Unit = {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child)
    }
  }

  /*
  def traceTaskParent(parent: AnyRef, child: Long): Unit = {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child)
    }
  }*/

  def traceTaskParent(parent: Long, child: Long): Unit = {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child)
    }
  }

  def enterSchedulable(s: Schedulable, t: SimpleWorkStealingScheduler.SchedulableExecutionType): Unit = {
    if (traceTasks) {
      Boundaries.enterSchedulable(s, t)
    }
  }

  def currentSchedulable: Schedulable = {
    if (traceTasks) {
      Boundaries.currentSchedulable
    } else {
      null
    }
  }

  def exitSchedulable(s: Schedulable): Unit = {
    if (traceTasks) {
      Boundaries.exitSchedulable(s)
    }
  }

  def exitSchedulable(s: Schedulable, old: Schedulable): Unit = {
    if (traceTasks) {
      Boundaries.exitSchedulable(s, old)
    }
  }

  object Boundaries {
    @TruffleBoundary(allowInlining = true) @noinline
    def shareSchedulableID(d: AnyRef, s: AnyRef): Unit = {
      SimpleWorkStealingScheduler.shareSchedulableID(d, s)
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def getSchedulableID(s: AnyRef) = {
      SimpleWorkStealingScheduler.getSchedulableID(s)
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def traceTaskParent(parent: AnyRef, child: AnyRef): Unit = {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def traceTaskParent(parent: AnyRef, child: Long): Unit = {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def traceTaskParent(parent: Long, child: Long): Unit = {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def enterSchedulable(s: Schedulable, t: SimpleWorkStealingScheduler.SchedulableExecutionType): Unit = {
      SimpleWorkStealingScheduler.enterSchedulable(s, t)
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def currentSchedulable: Schedulable = {
      SimpleWorkStealingScheduler.currentSchedulable
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def exitSchedulable(s: Schedulable): Unit = {
      SimpleWorkStealingScheduler.exitSchedulable(s)
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def exitSchedulable(s: Schedulable, old: Schedulable): Unit = {
      SimpleWorkStealingScheduler.exitSchedulable(s, old)
    }
  }
}
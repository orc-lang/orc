package orc.run.porce.instruments

import java.util.ArrayDeque
import java.util.concurrent.atomic.LongAdder

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

object ProfilerUtils {
  abstract class ProfilerBase {
    val profilerState = new ThreadLocal[ProfilerState] {
      override def initialValue(): ProfilerState = {
        new ProfilerState()
      }
    }
  
    @TruffleBoundary(allowInlining = true) @noinline
    def getProfilerState() = {
      profilerState.get()
    }
  }
  
  class ProfilerState {
    val counterStack = new ArrayDeque[Counter]();

    @TruffleBoundary(allowInlining = true) @noinline
    def popCurrentCounter() = {
      counterStack.pop()
      counterStack.peek()
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def pushCurrentCounter(c: Counter) = {
      if (c != counterStack.peek()) {
        counterStack.push(c);
        true
      } else {
        false
      }
    }

    val startTimeStack = new ArrayDeque[Long]();

    @TruffleBoundary(allowInlining = true) @noinline
    def popStartTime() = {
      startTimeStack.pop()
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def pushStartTime(t: Long): Unit = {
      startTimeStack.push(t)
    }

  }

  class Counter {
    private val hits = new LongAdder()
    private val time = new LongAdder()
    private val childTime = new LongAdder()
    
    @TruffleBoundary(allowInlining = true) @noinline
    def reset() = {
      hits.reset()
      time.reset()
      childTime.reset()
    }

    @TruffleBoundary(allowInlining = true) @noinline
    def addHit(): Unit = {
      hits.increment()
    }
    
    @TruffleBoundary(allowInlining = true) @noinline
    def addTime(time: Long): Unit = {
      this.time.add(time)
    }
    @TruffleBoundary(allowInlining = true) @noinline
    def addChildTime(time: Long): Unit = {
      childTime.add(time)
    }
    @TruffleBoundary(allowInlining = true) @noinline
    def getSelfTime() = {
      time.sum() - childTime.sum()
    }
    @TruffleBoundary(allowInlining = true) @noinline
    def getTime() = {
      time.sum()
    }
    @TruffleBoundary(allowInlining = true) @noinline
    def getHits() = {
      hits.sum()
    }
    @TruffleBoundary(allowInlining = true) @noinline
    override def toString(): String = {
      s"hits = ${getHits()}, self time = ${toSeconds(getSelfTime())}s, total time = ${toSeconds(getTime())}s";
    }
  }

  def toSeconds(ns: Long) = {
    ns.toDouble / 1000 / 1000 / 1000
  }
}
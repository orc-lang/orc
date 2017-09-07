package orc.run.porce.instruments

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong
import java.util.HashMap
import orc.ast.porc.PorcAST
import scala.collection.JavaConverters._
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env
import com.oracle.truffle.api.vm.PolyglotEngine
import java.io.PrintWriter
import orc.util.CsvWriter

class PorcNodeExecutionProfiler(env: Env) {
  import PorcNodeExecutionProfiler._
  
  val profilerState = new ThreadLocal[ProfilerState] {
    override def initialValue(): ProfilerState = {
      new ProfilerState()
    }
  }

  @TruffleBoundary(allowInlining = true) @noinline
  def getProfilerState() = {
    profilerState.get()
  }

  def dispose(): Unit = {
    dump()
  }
  
  
  def dump(): Unit = synchronized {
    val out = new PrintWriter(env.out())
    val csv = new CsvWriter(out.write(_))
    csv.writeHeader(Seq("Type", "Porc Expression", "Hits", "Self Time", "Total Time"))
    for (entry <- nodeCounts.entrySet().asScala) {
      val k = entry.getKey();
      val count = entry.getValue();
      if (count.getHits() > 0) {
        import orc.util.StringExtension._
        csv.writeRow(Seq(k.getClass.getSimpleName, k.toString().replace('\n', ' ').truncateTo(200), count.getHits(), count.getSelfTime(), count.getTime()))
      }
    }
    out.flush();
  }

  def reset(): Unit = synchronized {
    for (c <- nodeCounts.values().asScala) {
      c.reset()
    }
  }

  val nodeCounts = new HashMap[PorcAST, Counter]();

  @TruffleBoundary(allowInlining = true) @noinline
  def getCounter(n: PorcAST): Counter = synchronized {
    nodeCounts.computeIfAbsent(n, (_) => new Counter())
  }
}

object PorcNodeExecutionProfiler {
  /** Finds profiler associated with given engine. There is at most one profiler associated with
    * any {@link PolyglotEngine}. One can access it by calling this static method.
    */
  def get(engine: PolyglotEngine): PorcNodeExecutionProfiler = {
    val instrument = engine.getRuntime().getInstruments().get(PorcNodeExecutionProfilerInstrument.ID);
    if (instrument == null) {
      throw new IllegalStateException();
    }
    return instrument.lookup(classOf[PorcNodeExecutionProfiler]);
  }

  val KEY = PorcNodeExecutionProfiler;
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
    private val hits = new AtomicLong(0L)
    private val time = new AtomicLong(0L)
    private val childTime = new AtomicLong(0L)
    
    def reset() = {
      hits.set(0)
      time.set(0)
      childTime.set(0)
    }

    def addHit(): Unit = {
      hits.getAndIncrement()
    }

    def addTime(time: Long): Unit = {
      this.time.getAndAdd(time)
    }

    def addChildTime(time: Long): Unit = {
      childTime.getAndAdd(time)
    }

    def getSelfTime() = {
      time.get() - childTime.get()
    }

    def getTime() = {
      time.get()
    }

    def getHits() = {
      hits.get()
    }

    override def toString(): String = {
      s"hits = ${getHits()}, self time = ${toSeconds(getSelfTime())}s, total time = ${toSeconds(getTime())}s";
    }
  }

  def toSeconds(ns: Long) = {
    ns.toDouble / 1000 / 1000 / 1000
  }
}
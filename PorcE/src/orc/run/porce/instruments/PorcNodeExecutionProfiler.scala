package orc.run.porce.instruments

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import java.util.ArrayDeque
import java.util.HashMap
import orc.ast.porc.PorcAST
import scala.collection.JavaConverters._
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env
import com.oracle.truffle.api.vm.PolyglotEngine
import java.io.PrintWriter
import orc.util.CsvWriter
import java.util.concurrent.atomic.LongAdder
import orc.util.ExecutionLogOutputStream
import java.io.OutputStreamWriter

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

  @TruffleBoundary(allowInlining = true) @noinline
  def dispose(): Unit = {
    val out = ExecutionLogOutputStream("porc-profile-dispose", "csv", "Porc profile dump")
    if (out.isDefined) {
      val pout = new PrintWriter(new OutputStreamWriter(out.get))
      dump(pout)
    }
  }
  
  
  @TruffleBoundary(allowInlining = true) @noinline
  def dump(out: PrintWriter): Unit = synchronized {
    //val out = new PrintWriter(env.out())
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

  @TruffleBoundary(allowInlining = true) @noinline
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
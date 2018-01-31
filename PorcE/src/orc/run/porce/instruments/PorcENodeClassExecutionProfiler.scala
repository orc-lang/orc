package orc.run.porce.instruments

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import java.util.HashMap
import orc.ast.porc.PorcAST
import scala.collection.JavaConverters._
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env
import com.oracle.truffle.api.vm.PolyglotEngine
import java.io.PrintWriter
import orc.util.CsvWriter
import orc.util.ExecutionLogOutputStream
import java.io.OutputStreamWriter
import orc.run.porce.instruments.ProfilerUtils.ProfilerBase
import com.oracle.truffle.api.nodes.Node

class PorcENodeClassExecutionProfiler(env: Env) extends ProfilerBase {
  import ProfilerUtils._
  
  @TruffleBoundary(allowInlining = true) @noinline
  def dispose(): Unit = {
    val out = ExecutionLogOutputStream("porce-class-profile-dispose", "csv", "PorcE profile dump")
    if (out.isDefined) {
      val pout = new PrintWriter(new OutputStreamWriter(out.get))
      dump(pout)
    }
  }
  
  
  @TruffleBoundary(allowInlining = true) @noinline
  def dump(out: PrintWriter): Unit = synchronized {
    //val out = new PrintWriter(env.out())
    val csv = new CsvWriter(out.write(_))
    csv.writeHeader(Seq("Class [class]", "Hits [hits]", "Self Time (ns) [self]", "Total Time (ns) [total]"))
    for (entry <- nodeCounts.entrySet().asScala) {
      val k = entry.getKey();
      val count = entry.getValue();
      if (count.getHits() > 0) {
        csv.writeRow(Seq(k.getName.toString(), count.getHits(), count.getSelfTime(), count.getTime()))
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

  val nodeCounts = new HashMap[Class[_], Counter]();

  @TruffleBoundary(allowInlining = true) @noinline
  def getCounter(n: Class[_]): Counter = synchronized {
    nodeCounts.computeIfAbsent(n, (_) => new Counter())
  }
}

object PorcENodeClassExecutionProfiler {
  /** Finds profiler associated with given engine. There is at most one profiler associated with
    * any {@link PolyglotEngine}. One can access it by calling this static method.
    */
  def get(engine: PolyglotEngine): PorcENodeClassExecutionProfiler = {
    val instrument = engine.getRuntime().getInstruments().get(PorcENodeClassExecutionProfilerInstrument.ID);
    if (instrument == null) {
      throw new IllegalStateException();
    }
    return instrument.lookup(classOf[PorcENodeClassExecutionProfiler]);
  }

  val KEY = PorcENodeClassExecutionProfiler;
  
  def nonTrivialNode(n: Node): Boolean = {
    n match {
      case _: orc.run.porce.Read.Argument => false
      case _: orc.run.porce.Read.Local => false
      case _: orc.run.porce.Read.Closure => false
      case _: orc.run.porce.Write.Local => false
      case _ =>
        true
    }
  }
}
//
// PorcNodeExecutionProfiler.scala -- Scala class and object PorcNodeExecutionProfiler
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.instruments

import java.io.{ OutputStreamWriter, PrintWriter }
import java.util.HashMap

import scala.collection.JavaConverters.{ asScalaSetConverter, collectionAsScalaIterableConverter }

import orc.ast.porc.PorcAST
import orc.run.porce.instruments.ProfilerUtils.ProfilerBase
import orc.util.{ CsvWriter, ExecutionLogOutputStream }

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env
import com.oracle.truffle.api.vm.PolyglotEngine

class PorcNodeExecutionProfiler(env: Env) extends ProfilerBase {
  import ProfilerUtils._

  @TruffleBoundary @noinline
  def dispose(): Unit = {
    val out = ExecutionLogOutputStream("porc-profile-dispose", "csv", "Porc profile dump")
    if (out.isDefined) {
      val pout = new PrintWriter(new OutputStreamWriter(out.get))
      dump(pout)
    }
  }
  
  
  @TruffleBoundary @noinline
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

  @TruffleBoundary @noinline
  def reset(): Unit = synchronized {
    for (c <- nodeCounts.values().asScala) {
      c.reset()
    }
  }

  val nodeCounts = new HashMap[PorcAST, Counter]();

  @TruffleBoundary @noinline
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
}

package orc

import com.oracle.truffle.api.vm.PolyglotRuntime
import com.oracle.truffle.api.vm.PolyglotEngine

//import scala.collection.JavaConverters._
import orc.run.porce.PorcELanguage
import com.oracle.truffle.api.source.Source
import orc.script.OrcBindings
import java.io.File
import java.io.PrintWriter
import orc.util.ExecutionLogOutputStream
import java.io.OutputStreamWriter
import orc.util.DumperRegistry

object PorcEPolyglotLauncher {
  class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions
  
  var orcOptions: Option[OrcOptions] = None
  
  def main(args: Array[String]): Unit = {
    val options = new OrcCmdLineOptions()
    options.parseCmdLine(args)
    orcOptions = Some(options)
    val runtime = PolyglotRuntime.newBuilder().setOut(System.out).setErr(System.err).build()
    val engine = PolyglotEngine.newBuilder().runtime(runtime).build()

    val profilePorcNodes = false && System.getProperty("orc.executionlog.dir") != null
    
    if (profilePorcNodes) {
      import orc.run.porce.instruments.PorcNodeExecutionProfiler
      import orc.run.porce.instruments.PorcNodeExecutionProfilerInstrument

      val insts = runtime.getInstruments()

      insts.get(PorcNodeExecutionProfilerInstrument.ID).setEnabled(true)
      val profiler = PorcNodeExecutionProfiler.get(engine)      
      
      DumperRegistry.register { name =>
        val out = ExecutionLogOutputStream(s"porc-profile-$name", "csv", "Porc profile dump")
        if (out.isDefined) {
          val pout = new PrintWriter(new OutputStreamWriter(out.get))
          profiler.dump(pout)
        }
        profiler.reset()
      }
    }
    
    val profilePorcENodes = false && System.getProperty("orc.executionlog.dir") != null
    
    if (profilePorcENodes) {
      import orc.run.porce.instruments.PorcENodeExecutionProfiler
      import orc.run.porce.instruments.PorcENodeExecutionProfilerInstrument

      val insts = runtime.getInstruments()

      insts.get(PorcENodeExecutionProfilerInstrument.ID).setEnabled(true)
      val profiler = PorcENodeExecutionProfiler.get(engine)      
      
      DumperRegistry.register { name =>
        val out = ExecutionLogOutputStream(s"porce-profile-$name", "csv", "Porc profile dump")
        if (out.isDefined) {
          val pout = new PrintWriter(new OutputStreamWriter(out.get))
          profiler.dump(pout)
        }
        profiler.reset()
      }
    }
    
    val profilePorcEClasses = false && System.getProperty("orc.executionlog.dir") != null
    
    if (profilePorcEClasses) {
      import orc.run.porce.instruments.PorcENodeClassExecutionProfiler
      import orc.run.porce.instruments.PorcENodeClassExecutionProfilerInstrument

      val insts = runtime.getInstruments()

      insts.get(PorcENodeClassExecutionProfilerInstrument.ID).setEnabled(true)
      val profiler = PorcENodeClassExecutionProfiler.get(engine)
      
      DumperRegistry.register { name =>
        val out = ExecutionLogOutputStream(s"porce-class-profile-$name", "csv", "PorcE class profile dump")
        if (out.isDefined) {
          val pout = new PrintWriter(new OutputStreamWriter(out.get))
          profiler.dump(pout)
        }
        profiler.reset()
      }
    }

    engine.eval(Source.newBuilder(new File(options.filename)).mimeType(PorcELanguage.MIME_TYPE).build())
    
    engine.dispose()
    runtime.dispose()
    System.exit(0);
  }
}
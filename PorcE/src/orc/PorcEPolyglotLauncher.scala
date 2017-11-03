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

object PorcEPolyglotLauncher {
  class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions
  
  var orcOptions: Option[OrcOptions] = None
  
  def main(args: Array[String]): Unit = {
    val options = new OrcCmdLineOptions()
    options.parseCmdLine(args)
    orcOptions = Some(options)
    val runtime = PolyglotRuntime.newBuilder().setOut(System.out).setErr(System.err).build()
    val engine = PolyglotEngine.newBuilder().runtime(runtime).build()

    val profilePorcNodes = true && System.getProperty("orc.executionlog.dir") != null
    
    if (profilePorcNodes) {
      import orc.run.porce.instruments.PorcNodeExecutionProfiler
      import orc.run.porce.instruments.PorcNodeExecutionProfilerInstrument
      import java.util.Timer
      import java.util.TimerTask

      val insts = runtime.getInstruments()

      insts.get(PorcNodeExecutionProfilerInstrument.ID).setEnabled(true)
      val profiler = PorcNodeExecutionProfiler.get(engine)
      
      var i = 0
      
      val timer = new Timer(true)
      timer.scheduleAtFixedRate(new TimerTask {
        def run(): Unit = {
          val out = ExecutionLogOutputStream(s"porc-profile-$i", "csv", "Porc profile dump")
          i += 1
          if (out.isDefined) {
            val pout = new PrintWriter(new OutputStreamWriter(out.get))
            profiler.dump(pout)
            profiler.reset()
          }
        }
      }, 90 * 1000, 30 * 1000)
    }

    engine.eval(Source.newBuilder(new File(options.filename)).mimeType(PorcELanguage.MIME_TYPE).build())
    
    engine.dispose()
    runtime.dispose()
  }
}
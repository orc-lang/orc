package orc

import com.oracle.truffle.api.vm.PolyglotRuntime
import com.oracle.truffle.api.vm.PolyglotEngine

//import scala.collection.JavaConverters._
import orc.run.porce.PorcELanguage
import com.oracle.truffle.api.source.Source
import orc.script.OrcBindings
import java.io.File
//import orc.run.porce.instruments.PorcNodeExecutionProfiler
//import orc.run.porce.instruments.PorcNodeExecutionProfilerInstrument
//import java.util.Timer
//import java.util.TimerTask

object PorcEPolyglotLauncher {
  class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions
  
  var orcOptions: Option[OrcOptions] = None
  
  def main(args: Array[String]): Unit = {
    val options = new OrcCmdLineOptions()
    options.parseCmdLine(args)
    orcOptions = Some(options)
    val runtime = PolyglotRuntime.newBuilder().setOut(System.out).setErr(System.err).build()
    val engine = PolyglotEngine.newBuilder().runtime(runtime).build()
    //val insts = runtime.getInstruments()
    //val langs = engine.getLanguages()

    //println(insts.asScala)
    //println(langs.asScala)
    //println(options.filename)

    /*
    insts.get(PorcNodeExecutionProfilerInstrument.ID).setEnabled(true)
    val profiler = PorcNodeExecutionProfiler.get(engine)
    
    val timer = new Timer(true)
    timer.scheduleAtFixedRate(new TimerTask {
      def run(): Unit = {
        profiler.dump()
        profiler.reset()
      }
    }, 90 * 1000, 20 * 1000)
    */

    engine.eval(Source.newBuilder(new File(options.filename)).mimeType(PorcELanguage.MIME_TYPE).build())
    
    engine.dispose()
    runtime.dispose()
  }
}
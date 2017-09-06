package orc

import com.oracle.truffle.api.vm.PolyglotRuntime
import com.oracle.truffle.api.vm.PolyglotEngine

import scala.collection.JavaConverters._
import orc.run.porce.PorcELanguage
import com.oracle.truffle.api.source.Source
import orc.script.OrcBindings
import java.io.File
//import orc.util.PorcNodeExecutionProfiler

object PorcEPolyglotLauncher {
  class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions
  
  var orcOptions: Option[OrcOptions] = None
  
  def main(args: Array[String]): Unit = {
    val options = new OrcCmdLineOptions()
    options.parseCmdLine(args)
    orcOptions = Some(options)
    val runtime = PolyglotRuntime.newBuilder().setOut(System.out).setErr(System.err).build()
    val engine = PolyglotEngine.newBuilder().runtime(runtime).build()
    val insts = runtime.getInstruments()
    val langs = engine.getLanguages()

    //println(insts.asScala)
    //println(langs.asScala)
    //println(options.filename)
    
    //insts.get(PorcNodeExecutionProfiler.ID).setEnabled(true)
    
    engine.eval(Source.newBuilder(new File(options.filename)).mimeType(PorcELanguage.MIME_TYPE).build())
    
    println("Done")
    
    engine.dispose()
    runtime.dispose()
  }
}
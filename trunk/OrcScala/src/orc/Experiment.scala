package orc

import orc.compile.StandardOrcCompiler
import orc.run.StandardOrcRuntime
import orc.compile.parse.OrcReader
import orc.run._
import orc.values.Value
import orc.values.sites.Site
import orc.values.Format
import scala.concurrent.SyncVar

object ExperimentOptions extends OrcOptions {
  var filename = ""
  var debugLevel = 0
  var shortErrors = false

  // Compile options
  var usePrelude = true
  var includePath: java.util.List[String] = { val r = new java.util.ArrayList[String](1); r.add("."); r } 
  var additionalIncludes: java.util.List[String] = new java.util.ArrayList[String](0)
  var exceptionsOn = false
  var typecheck = false
  var quietChecking = false

  // Execution options
  var maxPublications = -1
  var tokenPoolSize = -1
  var stackSize = -1
  var classPath: java.util.List[String] = new java.util.ArrayList[String](0)
  def hasCapability(capName: String) = false
  def setCapability(capName: String, newVal: Boolean) { }
}

object Experiment {

  def main(args: Array[String]) {
    if (args.length < 1) {
      throw new Exception("Please supply a source file name as the first argument.\n" +
                          "Within Eclipse, use ${resource_loc}")
    }
    ExperimentOptions.filename = args(0)
    val orc = new StandardOrcRuntime()
    val compiler = new StandardOrcCompiler()
    val reader = OrcReader(new java.io.FileReader(ExperimentOptions.filename), ExperimentOptions.filename, compiler.openInclude(_, _, ExperimentOptions))
    val compiledOil = compiler(reader, ExperimentOptions)
    if (compiledOil != null) {
      try {
      orc.runSynchronous(compiledOil, { v: AnyRef => println("Published: " + Format.formatValue(v)) })
      } finally {
        orc.stop // kill threads and reclaim resources
      }
    }
    else {
      Console.err.println("Compilation failed.")
    }
  }

}

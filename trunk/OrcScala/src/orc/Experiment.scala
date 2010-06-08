package orc

import orc.compile.OrcCompiler
import orc.compile.parse.OrcReader
import orc.run.Orc
import orc.values.Value
import orc.values.sites.Site

class ExperimentalOrc extends Orc {
  def emit(v: Value) { print("Published: " + v + "   = " + v.toOrcSyntax() + "\n") }
  def halted { print("Done. \n") }
  def invoke(t: this.Token, s: Site, vs: List[Value]) { s.call(vs,t) }
  def expressionPrinted(s: String) { print(s) }
  def schedule(ts: List[Token]) { for (t <- ts) t.run }
}

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

  val orc = new ExperimentalOrc 

  def main(args: Array[String]) {
    if (args.length < 1) {
      throw new Exception("Please supply a source file name as the first argument.\n" +
                          "Within Eclipse, use ${resource_loc}")
    }
    ExperimentOptions.filename = args(0)
    val compiler = new OrcCompiler()
    val reader = OrcReader(new java.io.FileReader(ExperimentOptions.filename), ExperimentOptions.filename, compiler.openInclude(_, _, ExperimentOptions))
    val compiledOil = compiler(reader, ExperimentOptions)
    if (compiledOil != null) {
      Console.err.println("Compilation result: \n" + compiledOil + "\n")
      orc.run(compiledOil)
    }
    else {
      Console.err.println("Compilation failed.")
    }
  }

}

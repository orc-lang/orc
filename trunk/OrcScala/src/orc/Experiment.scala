package orc

import java.io.StringReader
import orc.script.OrcBindings
import oil._
import orc.sites.Site

class ExperimentalOrc extends Orc {
  def emit(v: Value) { print("Published: " + v + "\n") }
  def halted { print("Done. \n") }
  def invoke(t: this.Token, s: Site, vs: List[Value]) { s.call(vs,t) }
  def schedule(ts: List[Token]) { for (t <- ts) t.run }
}

object ExperimentOptions extends OrcOptions {
  var filename = ""
  var debugLevel = 0
  var shortErrors = false

  // Compile options
  var noPrelude = false
  var includePath = List[String](".")
  var exceptionsOn = false
  var typecheck = false
  var quietChecking = false

  // Execution options
  var maxPublications = -1
  var tokenPoolSize = -1
  var stackSize = -1
  var classPath = List[String]()
  def hasCapability(capName: String) = false
  def setCapability(capName: String, newVal: Boolean) { }
}

object Experiment {

  val orc = new ExperimentalOrc 

  val orcTest = Parallel(
      Sequence(
          Constant(Literal(5)), 
          Constant(Literal(3))
      ),
      Sequence(
          Parallel(Constant(Literal(7)),Constant(Literal(8))),
          Parallel(Variable(0), Variable(0))
      )
  )

  val parseTest = "5 >> 3 | (7 | 8) >x> (x | x)"

    def main(args: Array[String]) {
    print((new OrcCompiler())(new StringReader(parseTest), ExperimentOptions))
    //orc.run(orcTest)
  }


}

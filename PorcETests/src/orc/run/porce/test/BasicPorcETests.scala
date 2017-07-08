package orc.run.porce.test

import orc.ast.porc
import orc.compiler.porce.PorcToPorcE
import orc.run.porce

object BasicPorcETests {
  def main(args: Array[String]) = {
    import porc._
    val c = new Variable("c")
    val a = new Variable("a")
    val p = Let(c, Continuation(Seq(a), Constant("42") ::: a), PorcUnit() ::: CallContinuation(c, Seq(Constant("6"))))
    println(p)
    val translator = new PorcToPorcE
    val e = translator(p)
    println(e)
    
    var x = ""
    for(_ <- 0 until 10000) {
      x += e.call(Array[Object]()).asInstanceOf[String]
    }
    
  }
}
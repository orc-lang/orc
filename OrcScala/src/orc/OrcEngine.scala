package orc

class OrcEngine {
  import orc.oil.Value
  import orc.oil.nameless.Expression
  import java.lang._
  import orc.sites.Site

  val out = new StringBuffer("")

  val orcRuntime = new Orc {
    def emit(v: Value) { out.append(v) }
    def halted { print("Done. \n") }
    def invoke(t: this.Token, s: Site, vs: List[Value]) { s.call(vs,t) }
    def expressionPrinted(s: String) { print(s) }
    def schedule(ts: List[Token]) { for (t <- ts) t.run }
  }

  def getOut() : StringBuffer = out

  def run(e : Expression) {
    orcRuntime.run(e)
  }

}

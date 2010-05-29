package orc


class OrcEngine {
  import oil._
  import java.lang._
  import orc.sites.Site
  
  val out = new StringBuffer("")
  
  val orcRuntime = new Orc {
    	def emit(v: Value) { out.append(v) }
    	def halted { print("Done. \n") }
    	def invoke(t: this.Token, s: Site, vs: List[Value]) { s.call(vs,t) }
    	def schedule(ts: List[Token]) { for (t <- ts) t.run }
    }

  def getOut() : StringBuffer = out
   
  def run(e : Expression) {
    orcRuntime.run(e)
  }
  
}

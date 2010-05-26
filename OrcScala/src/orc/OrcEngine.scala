package orc


class OrcEngine {
  import oil._
  import java.lang._
  
  val out = new StringBuffer("")
  
  val orc = new Orc {
    	def emit(v: Value) { out.append(v) }
    	def halted { print("Done. \n") }
    	def invoke(t: this.Token, s: Site, vs: List[Value]) { t.publish(Signal) }
    	def schedule(ts: List[Token]) { for (t <- ts) t.run }
    }

  def getOut() : StringBuffer = out
   
  def run(e : Expression) {
    orc.run(e)
  }
  
}




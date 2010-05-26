package orc


class OrcEngine {
  import oil._

  val orc = new Orc {
    	def emit(v: Value) { print("Published: " + v + "\n") }
    	def halted { print("Done. \n") }
    	def invoke(t: this.Token, s: Site, vs: List[Value]) { t.publish(Signal) }
    	def schedule(ts: List[Token]) { for (t <- ts) t.run }
    }

  def run(e : Expression) {
    orc.run(e)
  }
  
}




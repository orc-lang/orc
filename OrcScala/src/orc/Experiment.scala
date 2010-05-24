package orc

object Experiment {

  import oil._

  val orc = new Orc {
    	def emit(v: Value) { print("Published: " + v + "\n") }
    	def halted { print("Done. \n") }
    	def invoke(t: this.Token, s: Site, vs: List[Value]) { t.publish(Signal) }
    	def schedule(ts: List[Token]) { for (t <- ts) t.run }
    }

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
	  print(OrcParser.parse(parseTest))
      //orc.run(orcTest)
  }
  
  
}

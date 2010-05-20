object Experiment {

  import Oil._

  val orc = new Orc {
    	def emit(v: Value) { print(v) }
    	def halted { print("Done.") }
    	def invoke(t: this.Token, s: Site, vs: List[Value]) { t.publish(Signal) }
    	def schedule(ts: List[Token]) { for (t <- ts) t.run }
    }

  val test = Parallel(
		  		Sequence(
		  			Constant(Literal(5)), 
		  			Constant(Literal(3))
		  		),
  				Sequence(
  					Parallel(Constant(Literal(7)),Constant(Literal(8))),
  					Parallel(Variable(0), Variable(0))
  				)
      		)
  
  val p = "5 >> 3 | (7 | 8) >x> (x | x)"
      		
  def main(args: Array[String]) {
	  print(OrcParser.parse(p))
      //orc.run(test)
  }
  
  
}

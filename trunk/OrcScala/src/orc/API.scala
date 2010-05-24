package orc

trait TokenAPI {
	
	import oil.Value
	
	def publish(v : Value): Unit
	def halt: Unit
	
	def kill: Unit
	def run: Unit
}

trait OrcAPI {
	
	import oil._
	
	type Token <: TokenAPI
	
//	def start(e: Expression) : Unit
//	def pause
//	def resume
//	def stop
	
	def emit(v: Value): Unit
	def halted: Unit
	def invoke(t: Token, s: Site, vs: List[Value]): Unit
	def schedule(ts: List[Token]): Unit
	
	// Schedule function is overloaded for convenience
	def schedule(t: Token) { schedule(List(t)) }
	def schedule(t: Token, u: Token) { schedule(List(t,u)) }
}


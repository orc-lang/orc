package orc {
	
	
	abstract class Located {
		import scala.util.parsing.input.Position
		var location: Option[Position] = None
	}
	
	abstract class AST extends Located {
		
		def ->[B <: AST](f: this.type => B): B = {
			val location = this.location
			val result = f(this)
			result.location = location
			result
		}
		
		def !!(e : Located with Throwable) = {
			e.location = this.location
			throw e
		}
	}
	
}
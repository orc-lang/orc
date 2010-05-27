import scala.util.parsing.input.Positional

package orc {

	abstract class AST extends Positional {
		
		def ->[B <: AST](f: this.type => B): B = {
			val location = this.pos
			val result = f(this)
			result.pos = location
			result
		}
		
		def !!(e : Positional with Throwable) = {
			e.pos = this.pos
			throw e
		}
	}
	
}
package orc.oil.nameless

// Infix combinator constructors
trait NamelessInfixCombinators {
	  self: Expression =>
	
	// Infix combinator constructors
	def ||(g: Expression) =  Parallel(this,g)
	def >>(g: Expression) =  Sequence(this,g)
	def <<(g: Expression) =     Prune(this,g)
	def ow(g: Expression) = Otherwise(this,g)
}



// Infix combinator extractors
// Infix combinator extractors
object || {
	def apply(f: Expression, g: Expression) = Parallel(f,g)
	def unapply(e: Expression) =
		e match {
			case Parallel(l,r) => Some((l,r))
			case _ => None
		}
}

object >> {
	def unapply(e: Expression) =
		e match {
			case Sequence(l,r) => Some((l,r))
			case _ => None
		}
}

object << {
	def unapply(e: Expression) =
		e match {
			case Prune(l,r) => Some((l,r))
			case _ => None
		}
}

object ow {
	def unapply(e: Expression) =
		e match {
			case Otherwise(l,r) => Some((l,r))
			case _ => None
		}
}

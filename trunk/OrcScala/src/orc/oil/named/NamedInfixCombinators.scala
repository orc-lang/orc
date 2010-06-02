package orc.oil.named

// Infix combinator constructors
trait NamedInfixCombinators {
	  self: Expression =>
	
	def ||(g: Expression) = Parallel(this,g)
	
	def >>(g : Expression) = Sequence(this, new TempVar(), g)
	
	def >(x : TempVar) =
		new {
			def >(g: Expression) = Sequence(NamedInfixCombinators.this, x, g)
		}
	
		
    def <<(g : Expression) = Prune(this, new TempVar(), g)
    
	def <(x : TempVar) =
		new {
			def <(g: Expression) = Prune(NamedInfixCombinators.this, x, g)
		}
		
	def ow(g: Expression) = Otherwise(this,g)
}



// Infix combinator extractors
object || {
	def unapply(e: Expression) =
		e match {
			case Parallel(l,r) => Some((l,r))
			case _ => None
		}
}

object > {
	def unapply(e : Expression) = {
		e match {
			case Sequence(f,x,g) => Some( ( (f,x), g ) )
			case _ => None
		}
	}
	def unapply(p: (Expression, TempVar)) = Some(p)	
}

object < {
	def unapply(e : Expression) = {
		e match {
			case Prune(f,x,g) => Some( ( (f,x), g ) )
			case _ => None
		}
	}
	def unapply(p: (Expression, TempVar)) = Some(p)	
}

object ow {
	def unapply(e: Expression) =
		e match {
			case Otherwise(l,r) => Some((l,r))
			case _ => None
		}
}

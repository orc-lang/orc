package orc.sites {
	
	import orc.oil.Value
	import orc.oil.Type
	import orc.TokenAPI
	
	trait Site extends Value {
		def call(args: List[Value], token: TokenAPI): Unit
		def typecheck(argTypes: List[Type]): Type
	}
	
	trait Partial {
		def call(args: List[Value], token: TokenAPI) {
			evaluate(args) match {
				case Some(v) => token.publish(v)
				case None => token.halt
			}
		}
		
		def evaluate(args: List[Value]): Option[Value]
	}
	
	trait Untyped {
		def typecheck(argTypes: List[Type]): Type = orc.oil.Bot()
	}
	
	
	package native {
		import orc.oil._
		
		object If extends Partial {
			def evaluate(args: List[Value]) =
				args match {
					case List(Literal(true)) => Some(Literal({}))
					case _ => None
				}
		}
		
		
	}
	 
	
	
	
}
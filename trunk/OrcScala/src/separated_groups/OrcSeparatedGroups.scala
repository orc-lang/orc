abstract class OrcSeparatedGroups extends OrcAPI with Groups {

	import Oil._
	import PartialMapExtension._
	
	var exec: Option[Execution] = Some(new Execution())
	
	def run(node: Expression) {
		val exec = new Execution()
		val t = new Token(node, exec)
		t.run
	}
	
	// An execution is a special toplevel group, 
	// associated with the entire program.
	class Execution extends Group {

	def publish(t: Token, v: Value) {
		emit(v)
		t.halt
	}

	def onHalt {
		halted
	}
	}
	
	
	
// Tokens and their auxilliary structures //
	
	
	// Context entries //
	trait Binding
	implicit def ValuesAreBindings(v: Value): Binding = (v: Binding)
	implicit def GroupcellsAreBindings(g: Groupcell): Binding = (g: Binding)
	
	
	// Closures //
	class Closure(d: Def) extends Value {
		val arity: Int = d.arity
		val body: Expression = d.body
		var context: List[Binding] = Nil
	}
	object Closure {
		def unapply(c: Closure) = Some((c.arity, c.body, c.context))
	}

	
	
	// Control Frames //
	abstract class Frame extends ((Token, Value) => Unit)

	case class BindingFrame(n: Int = 1) extends Frame {
		def apply(t: Token, v: Value) {
			t.env = t.env.drop(n)
			t.publish(v)
		}
	}
	
	case class SequenceFrame(node: Expression) extends Frame {
		def apply(t: Token, v: Value) {
			schedule(t.bind(v).move(node))
		}
	}
	
	case class FunctionFrame(callpoint: Expression, env: List[Binding]) extends Frame {
		def apply(t: Token, v: Value) {
			t.env = env
			t.move(callpoint).publish(v)
		}
	}
	
	case object GroupFrame extends Frame {
		def apply(t: Token, v: Value) {
			t.group.publish(t,v)
		}
	}




	// Token //
	
	class TokenState
	case object Live extends TokenState
	case object Halted extends TokenState
	case object Killed extends TokenState
	
	class Token private (
			var node: Expression,
			var stack: List[Frame] = Nil,
			var env: List[Binding] = Nil,
			var group: Group,
			var state: TokenState = Live
	) extends TokenAPI
	{	
	
		def this(start: Expression, exec: Execution) = {
			this(node = start, group = exec)
		}
	
		// Copy constructor with defaults
		private def copy(
			node: Expression = node,
			stack: List[Frame] = stack,
			env: List[Binding] = env,
			group: Group = group,
			state: TokenState = state): Token = 
			{
				new Token(node, stack, env, group, state)
			}
		
	
		
		def fork = (this, copy())
		
		def move(e: Expression) = { node = e ; this }
		
		def push(f: Frame) = { stack = f::stack ; this }
		
		def join(child: Group) = { 
			val parent = group
			child.add(this); parent.remove(this)
			group = child
			push(GroupFrame)
			this 
		}			
		
		// Manipulating context frames
	
		def bind(b: Binding): Token = {
			env = b::env
			stack match {
			case BindingFrame(n)::fs => { stack = (new BindingFrame(n+1))::fs }
			case fs => { stack = BindingFrame()::fs }
			}
			this
		}
	
		def lookup(a: Argument): Binding = 
			a match {
			case Constant(v) => v
			case Variable(x) => env(x)
		}
	
		def resolve(a: Argument): Option[Value] =
			lookup(a) match {
				case (v: Value) => Some(v)
				case (g: Groupcell) => g.read(this)
			}
	
	
	
		// Publicly accessible methods
	
		def publish(v: Value) {
			stack match {
			case f::fs => { 
				stack = fs
				f(this, v)
			}
			case Nil => { emit(v) }
			}
		}
	
		def halt {
			state match {
			case Live => { state = Halted }
			case _ => {  }
			}
		}
	
		def kill {
			state match {
			case Live => { state = Killed }
			case _ => {  }
			}
		}
	
		// Run this token.
	
		def run {
			if (state == Live)
				node match {
				case Stop => halt
				case (a: Argument) => resolve(a).foreach(publish(_))
				case Call(target, args) => {
					resolve(target).foreach({
					case Closure(arity, body, newcontext) => {
						if (arity == args.size) {
							/* Caution: The ordering of these statements is very important; do not permute them. */
							val frame = new FunctionFrame(node, env)
							this.env = newcontext
							this.push(frame)
							for (a <- args) { bind(lookup(a)) }
							this.move(body).run				  					
						}
						else halt /* arity mismatch */
					}
					case (s: Site) => {
						val vs = args.partialMap(resolve)
						vs.foreach(invoke(this,s,_))
					}
					case _ => halt /* uncallable value */
					})
				}
	
				case Parallel(left, right) => {
					val (l,r) = fork
					schedule(l.move(left), r.move(right))		
				}
	
				case Sequence(left, right) => {
					val frame = new SequenceFrame(right)		  	  
					this.push(frame).move(left).run
				}
	
				case Prune(left, right) => {
					val (l,r) = fork
					val groupcell = Groupcell(group)
					schedule( l.bind(groupcell).move(left),
							  r.join(groupcell).move(right) )
				}
	
				case Cascade(left, right) => {
					val (l,r) = fork
					val region = Region(group, r)
					l.join(region).move(left).run
				}
	
				case DeclareDefs(defs, body) => {
					val cs = defs map ( (d: Def) => new Closure(d) )
					for (c <- cs) { bind(c) }
					for (c <- cs) { c.context = this.env }
					this.move(body).run
				}
	
			}
		}
	
	}

	
	
	
	
}
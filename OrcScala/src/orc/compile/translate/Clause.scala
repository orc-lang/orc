package orc.compile.translate

import orc.compile.ext._
import orc.oil.named
import orc.compile.translate.PrimitiveForms._

case class Clause(formals: List[Pattern], body: Expression) extends orc.AST {
	
	val arity = formals.size
	
	/**
	 * 
	 * Convert a clause into a cascading match; if the clause patterns match,
	 * execute the body, otherwise execute the fallthrough expression.
	 * 
	 * The supplied args are the formal parameters of the overall function.
	 * 
	 */
	def convert(args: List[named.TempVar], fallthrough: named.Expression): named.Expression = {		
		
		val (strictPairs, nonstrictPairs) = (formals zip args) partition { case (p,_) => p.isStrict }
		var newbody = Translator.convert(body)
		
		// Map all of the nonstrict patterns directly.
		for ((p,x) <- nonstrictPairs) {
			val (_, scope) = Translator.convertPattern(p)
			newbody = scope(x)(newbody)
		}
		
		strictPairs match {
			/* 
			 * There are no strict patterns. 
			 * There is no possibility of a failed match, so we just ignore the fallthrough case.
			 */
			case Nil => {
				// Make sure the remaining cases are not redundant.
				fallthrough match {
					case named.Stop => {  }
					case _ => { fallthrough !! "Redundant match" }
				}
			}
			/* 
			 * There is exactly one strict pattern.
			 */
			case (strictPattern, strictArg) :: Nil => {
				val (filter, scope) = Translator.convertPattern(strictPattern)
				val source = filter(strictArg)
				val x = new named.TempVar()
				val target = scope(x)(newbody)
				
				newbody = source > x > target
			}
			/*
			 * There are multiple strict patterns.
			 */
			case _ => { 
				val (strictPatterns, strictArgs) = List unzip strictPairs
				val (filter, scope) = Translator.convertPattern(TuplePattern(strictPatterns))
				val source = filter(makeTuple(strictArgs))
				val x = new named.TempVar()
				val target = scope(x)(newbody)
				
				val z = new named.TempVar()
				val y = new named.TempVar()
				
				
				newbody = (    (source  > z >  callSome(z)) 
							ow ( callNone() )
						  ) > y >
						  (    (callIsSome(y)  > x >  target)
						    || (callIsNone(y) >> fallthrough)   
						  )
				
			}
		}  
		
		/* Finally, return the newly constructed expression */
		newbody
	}
	
}

object Clause {

  /**
   * If these clauses all have the same arity, return that arity.
   * Otherwise, throw an exception.
   * 
   * The list of clauses is assumed to be nonempty.
   */
  def commonArity(clauses: List[Clause]): Int = {
	val first :: rest = clauses
	
	rest find { _.arity != first.arity } match {
	  case Some(clause) => clause !! "Clause has wrong number of parameters."
	  case None => first.arity
	}
  }
  
  /**
   * Convert this list of clauses to a single expression
   * which linearly matches those clauses.
   * 
   * Also return the list of arguments against which the
   * converted body expression performs this match.
   * 
   * The list of clauses is assumed to be nonempty.
   */
  def convertClauses(clauses: List[Clause]): (List[named.TempVar], named.Expression) = {
		
	  val arity = commonArity(clauses)
	  val args = (for (_ <- 0 until arity) yield new named.TempVar()).toList
	  
	  val nil: named.Expression = named.Stop
	  def cons(clause: Clause, fail: named.Expression) = clause.convert(args, fail)
	  val body = clauses.foldRight(nil)(cons)
	  
	  (args, body)
	}
	
}
package orc {
package translation {
	
	
object Translator {
	
	import scala.collection.immutable._
	
	def translate(options: OrcOptions, extendedAST : ext.Expression): oil.Expression = {
		val simpleAST = convert(extendedAST, new TreeMap(), new TreeMap())	
		val oilAST = rename(simpleAST, Nil, Nil)
		oilAST
	}
	
	def generateEVar = new EVar()
	def generateTVar = new TVar()
	
	
	
	
	
	def unfold(es: List[Expression], makeCore: List[Argument] => Expression): Expression = {
		val bindings = for (e <- es) yield (generateEVar, e)
		val args = for ((x,_) <- bindings) yield x
		val core = makeCore(args)
		(bindings foldLeft core) { case (f,(x,g)) => Prune(f,x,g) }
	}
	
	def callCons(head: Argument, tail: Argument) = Call(Constant("Cons"), List(head, tail), None)
	def callIf(a: Argument) = Call(Constant(orc.lib.builtin.If), List(a), None)
	def callNot(a : Argument) = Call(Constant(orc.lib.builtin.Not), List(a), None)
	def callEq(a : Argument, b : Argument) = Call(Constant("Eq"), List(a,b), None)
	def callNth(a : Argument, i : Int) = Call(a, List(Constant(i)), None)
	def callIsCons(a : Argument) = Call(Constant("IsCons"), List(a), None)
	def callUnapply(constructor : Argument, a : Argument) = {
		val extractor = generateEVar
		val getExtractor = Call(Constant("Unapply"), List(constructor), None)
		val invokeExtractor = Call(extractor, List(a), None)
		Sequence(getExtractor, extractor, invokeExtractor)
	}
	
	
	def makeLet(args: List[Argument]): Expression = {
		args match {
			case Nil => Constant({})
			case List(a) => a
			case _ => makeTuple(args)
		}
	}
	
	def makeTuple(elements: List[Argument]) = Call(Constant("Tuple"), elements, None)
	
	def makeList(elements: List[Argument]) = {
		val nil: Expression = new Constant(Nil)
		def cons(h: Argument, t: Expression): Expression = {
			val y = generateEVar
			Sequence(t, y, callCons(h, y))
		}
		elements.foldRight(nil)(cons)
	}
	
	
	
	
	
	def defPartition(e: ext.Expression): (List[ext.DefDeclaration], ext.Expression) = {
		e match {
			case ext.Declare(d: ext.DefDeclaration, f) => {
				val (ds, g) = defPartition(f)
				(d::ds, g)
			}
			case _ => (Nil, e)
		}
	}
	
	def formalsPartition(formals: List[ext.Pattern]): (List[ext.Pattern], Option[List[ext.Type]]) = {
		import PartialMapExtension._
		val maybePartitioned = 
			formals partialMap {
				case ext.TypedPattern(p, t) => Some(p, t)
				case _ => None
			}
		maybePartitioned match {
			case Some(l) => {
				val (ps, ts) = List unzip l
				(ps, Some(ts))
			}
			case None => (formals, None) 
		}
	}
	
	
	/* Decompose a pattern into two components:
		A sequence of operations which extract intermediate values from the source expression.
		A sequence of context bindings for the target expression.
	*/
	type PatternDecomposition = (List[(Expression, EVar)], List[(String,EVar)])
	
	def convertPattern(p : ext.Pattern, x : EVar, context : Map[String, EVar], typecontext : Map[String, TVar]) = {
	
		def decomposePattern(p : ext.Pattern, x: EVar): PatternDecomposition = {
			p match {
				case ext.Wildcard() => (Nil, Nil)
				case ext.ConstantPattern(c) => {
					val testexpr = callEq(x, Constant(c))
					val guard = (testexpr, generateEVar)
					(List(guard), Nil)
				}
				case ext.VariablePattern(name) => {
					val binding = (name, x)
					(Nil, List(binding))
				}
				case ext.TuplePattern(ps) => {
					val vars = (for (_ <- ps) yield generateEVar).toList
					val exprs = List.range(0, ps.length) map (callNth(x, _))
					val tupleCompute = exprs zip vars
					val subResults = List.map2(ps, vars)(decomposePattern)
					val (subCompute, subBindings): PatternDecomposition = List unzip subResults
					(tupleCompute ::: subCompute, subBindings)
				}
				case ext.ListPattern(Nil) => decomposePattern(ext.ConstantPattern(Nil), x)
				case ext.ListPattern(ps) => {
					val seed : ext.Pattern = ext.ListPattern(Nil)
					val folded = (ps foldRight seed)(ext.ConsPattern)	
					decomposePattern(folded, x)
				}
				case ext.ConsPattern(ph,pt) => {
					val y = generateEVar
					val consCompute = (callIsCons(x), y)
					val (subCompute, subBindings) = decomposePattern(ext.TuplePattern(List(ph,pt)), y)
					(consCompute :: subCompute, subBindings)
				}
				case ext.CallPattern(name, args) => {
					val y = generateEVar 
					val matchCompute = (callUnapply(context(name), x), y)
					val (subCompute, subBindings) = decomposePattern(ext.TuplePattern(args), y)
					(matchCompute :: subCompute, subBindings)
				}
				case ext.AsPattern(p, name) => {
					val binding = (name, x)
					val (subCompute, subBindings) = decomposePattern(p, x)
					(subCompute, binding :: subBindings)
				}
				case ext.EqPattern(name) => {
					val testexpr = callEq(x, context(name))
					val guard = (testexpr, generateEVar)
					(List(guard), Nil)
				}
				// Slightly inefficient but very easy to read
				case ext.TypedPattern(p, t) => {
					val y = generateEVar
					val newt = convertType(t, typecontext)
					val typedCompute = (HasType(x, newt), y)
					val (subCompute, subBindings) = decomposePattern(p, y)
					(typedCompute :: subCompute, subBindings) 
				}			
			}
		}
	
		val (entireCompute, entireBindings) = decomposePattern(p, x)
		
		val (names, _) = List unzip entireBindings
		for (name <- names) {
			if (names exists (_ equals name)) {
				throw new Exception("Nonlinear pattern: " + name + " occurs more than once.")
			}
		}
		
		(entireCompute, entireBindings)
	}
	
	
	
	def combinatorPattern(combinator: (Expression, EVar, Expression) => Expression,
						  source: ext.Expression,
						  p : ext.Pattern,
						  target: ext.Expression, 
						  context : Map[String, EVar], 
						  typecontext : Map[String, TVar]): Expression = 
	{ 
		val sourceVar = generateEVar
		val (compute, bindings) = convertPattern(p, sourceVar, context, typecontext)
		val fragments = (for ((_,z) <- bindings) yield z).toList.removeDuplicates
		
		val nil = makeLet(fragments)
		def cons(oneCompute: (Expression, EVar), expr2: Expression) = {
			val (expr1, x) = oneCompute
			Sequence(expr1, x, expr2)
		}
		val convertedSource = convert(source, context, typecontext)
		val newCompute = (convertedSource, sourceVar)::compute
		val newsource = newCompute.foldRight(nil)(cons)
		
		val mappedVar = generateEVar
		val mapping = Map() ++ (fragments map (x => (x,generateEVar)))
		val newbindings = bindings map { case (name,z) => (name, mapping(z)) }
		
		val nil2 = convert(target, context ++ newbindings, typecontext)
		def cons2(indexedFragment: (EVar, Int), expr: Expression) = {
			val (z, i) = indexedFragment
 			combinator(callNth(mappedVar,i), mapping(z), expr)
		}
		val indexedFragments = fragments.zipWithIndex
		val newtarget = indexedFragments.foldRight(nil2)(cons2)
		 
		combinator(newsource, mappedVar, newtarget)
	}
	
	
	
	
	type Clause = (List[ext.Pattern], ext.Expression)
	type Sig = (Option[List[String]], Option[List[ext.Type]], Option[ext.Type])
	
	
	// Incomplete.
	def convertClause(context: Map[String, EVar], typecontext: Map[String, TVar])
	 				 (args: List[EVar])
					 (clause: Clause, fallthrough: Expression): Expression = {
		
		val (ps, body) = clause
		
		val (groupedComputes, groupedBindings) = 
			List unzip {
				(ps zip args) map 
					{ pair => 
						val (p,x) = pair
						convertPattern(p, x, context, typecontext)
					}
			}
		val _ = groupedComputes.flatten
		val bindings = groupedBindings.flatten
		
		val fragments = (for ((_,z) <- bindings) yield z).toList.removeDuplicates -- args
		
		convert(body, context ++ bindings, typecontext)		
	}
	
	
	
	
	
	
	def convertClauses(clauses: List[Clause], args: List[EVar], context: Map[String, EVar], typecontext: Map[String, TVar]): Expression = {
		val nil: Expression = Stop()
		val cons = convertClause(context,typecontext)(args)_
		clauses.foldRight(nil)(cons)
	}
	
	
	
	def convertDefs(defs: List[ext.DefDeclaration], context: Map[String, EVar], typecontext: Map[String, TVar]): (List[Def], Map[String, EVar]) = {
		
		import scala.collection.mutable._
		val clausesMap : Map[String, List[Clause]] = new HashMap()
		val sigMap    : Map[String, Sig] = new HashMap()
		
		def unify[A](x: Option[A], y: Option[A]) = (x,y) match {
			case (None, None) => None
			case (Some(_), None) => x
			case (None, Some(_)) => y
			case _ => throw new Exception("Redundant type information")
		}
		
		for (d <- defs) d match {
			case ext.Def(name, formals, body, maybeReturnType) => {
				val (newformals, maybeArgTypes) = formalsPartition(formals)
				val newclause = (newformals, body)
				clausesMap.get(name) match {
					case None => 
						clausesMap update (name, List(newclause))
					case Some(clauses) => 
						clausesMap update (name, newclause::clauses)
				}
				sigMap.get(name) match {
					case None => 
						sigMap update (name, (None, maybeArgTypes, maybeReturnType))
					case Some((typeFormals2, maybeArgTypes2, maybeReturnType2)) =>
						sigMap update (name, (typeFormals2, unify(maybeArgTypes, maybeArgTypes2), unify(maybeReturnType, maybeReturnType2)))
				}
			}
			case ext.DefSig(name, typeformals, argtypes, maybeReturnType) => { 
				sigMap.get(name) match {
					case None => 
						sigMap update (name, (Some(typeformals), Some(argtypes), maybeReturnType))
					case Some((None, None, maybeReturnType2)) =>
						sigMap update (name, (Some(typeformals), Some(argtypes), unify(maybeReturnType, maybeReturnType2)))
					case _ =>
						throw new Exception("Redundant type information")
				}
			}
		}		
				
		if (!(sigMap.keySet subsetOf clausesMap.keySet)) { throw new Exception("Unused function signature") } // Sanity check. TODO: Make more helpful.
		
		val newcontext = context ++ (for (name <- clausesMap.keys) yield (name, generateEVar))
		
		val newdefs = 
			(for (name <- clausesMap.keys) yield {
				val (typeformals, argtypes, returntype) = 
					sigMap(name) match {
						case (_,None,_) => throw new Exception("Arg types not fully specified for function")
						case (Some(typeformals), Some(argtypes), returntype) => (typeformals, argtypes, returntype)
						case (None, Some(argtypes), returntype) => (Nil, argtypes, returntype)
					}
				
				val newtypecontext = typecontext ++ (for (name <- typeformals) yield (name, generateTVar))
				val newtypeformals = typeformals map newtypecontext
				val newargtypes = argtypes map (convertType(_, newtypecontext))
				val newreturntype = returntype map (convertType(_, newtypecontext))
				
				val args = newargtypes map (_ => generateEVar)
				val body = convertClauses(clausesMap(name), args, newcontext, newtypecontext)
				
				Def(newcontext(name), args, body, newtypeformals, newargtypes, newreturntype)
			}).toList
		
		(newdefs, newcontext)
	}
	
	
	
	
	
	
	def convertType(t : ext.Type, typecontext : Map[String, TVar]): Type = {
		t -> {
			case ext.Top() => Top()
			case ext.Bot() => Bot()
			case ext.NativeType(s) => NativeType(s)
			case ext.TupleType(ts) => TupleType(ts map (convertType(_,typecontext)))
			case ext.TypeVariable(name) => typecontext.getOrElse(name, throw new Exception("Unbound type variable"))
			case ext.FunctionType(typeformals, argtypes, returntype) => {
				val newTypeContext = typecontext ++ (for (x <- typeformals) yield (x, generateTVar))
				val newTypeFormals = typeformals map newTypeContext
				val newArgTypes = argtypes map (convertType(_, newTypeContext))
				val newReturnType = convertType(returntype, newTypeContext)
				FunctionType(newTypeFormals, newArgTypes, newReturnType)
			}
			case ext.TypeApplication(name, typeactuals) => {
				TypeApplication(typecontext(name), typeactuals map (convertType(_,typecontext)))
			}			
		}
	}
	
	
	
	
	// Incomplete for some cases.
	def convert(e : ext.Expression, context : Map[String, EVar], typecontext : Map[String, TVar]): Expression = {
			def converter(e : ext.Expression) = convert(e,context,typecontext)
			e -> {
				case ext.Stop() => Stop()
				case ext.Constant(c) => Constant(c)
				case ext.Variable(x) => context(x)
				case ext.TupleExpr(es) => unfold(es map converter, makeLet)
				case ext.ListExpr(es) => unfold(es map converter, makeList)
				//case ext.Call(target, gs) =>  
				case ext.PrefixOperator(op, arg) => {
					val opsite = Constant(op)
					unfold(List(arg) map converter, Call(opsite,_,None))
				}
				case ext.InfixOperator(l, op, r) => {
					val opsite = Constant(op)
					unfold(List(l,r) map converter, Call(opsite,_,None))
				}
				case ext.Sequential(l, None, r) => converter(l) >> converter(r)
				case ext.Sequential(l, Some(ext.VariablePattern(name)), r) => {
					val x = generateEVar
					Sequence(converter(l), x, convert(r, context + ((name, x)), typecontext)) 
				}
				case ext.Sequential(l, Some(p), r) => {
					combinatorPattern(Sequence, l, p, r, context, typecontext)
				}
				case ext.Parallel(l,r) => converter(l) || converter(r)
				case ext.Pruning(l, None, r) => converter(l) << converter(r)
				case ext.Pruning(l, Some(ext.VariablePattern(name)), r) => {
					val x = generateEVar
					Sequence(convert(l, context + ((name, x)), typecontext), x, converter(r)) 
				}
				case ext.Pruning(l, Some(p), r) => {
					def combinator(source: Expression, x: EVar, target: Expression) = Prune(target, x, source)
					combinatorPattern(combinator, r, p, l, context, typecontext)
				}
				case ext.Otherwise(l, r) => converter(l) semicolon converter(r)
				//case ext.Lambda(typeformals, formals, returntype, body) => 
				case ext.Conditional(ifE, thenE, elseE) => {
					 val t = generateEVar
					 val f = generateEVar
					 val ifexp = converter(ifE)
					 val thenbranch = callIf(t) >> converter(thenE)
					 val elsebranch = callIf(f) >> converter(elseE)
					 val conditional = thenbranch || elsebranch
					 Prune(Prune(conditional, f, callNot(t)), t, ifexp)
				}
				case ext.Declare(decl : ext.DefDeclaration, _) => {
					val (defs, remainder) = defPartition(e)
					val (newdefs, newcontext) = convertDefs(defs, context, typecontext)
					val newbody = convert(remainder, newcontext, typecontext)
					DeclareDefs(newdefs, newbody)
				}
				case ext.Declare(ext.Val(p,f), body) => {
					converter(ext.Pruning(body, Some(p), f))
				}
				
				/*
				  case ext.Declare(_, body) =>
					case class TypeAlias(name: String, typeformals: List[String] = Nil, aliasedtype: Type) extends Declaration
					case class Datatype(name: String, typeformals: List[String] = Nil, constructors: List[Constructor]) extends Declaration
					case class Constructor(name: String, types: List[Option[Type]]) extends Declaration
					case class TypeImport(name: String, classname: String) extends Declaration
				*/
				
				// Incomplete.
				case ext.Declare(ext.SiteImport(name, sitename), body) => converter(body)
				case ext.Declare(ext.ClassImport(name, classname), body) => converter(body)
				case ext.Declare(ext.Include(filename), body) => converter(body)
				
				case ext.TypeAscription(body, t) => HasType(converter(body),              convertType(t, typecontext)  )
				case  ext.TypeAssertion(body, t) => HasType(converter(body), AssertedType(convertType(t, typecontext)) )
				
		}
	}
	
	

	
	
	
	private def seek[A](x: A, xs: List[A]): Int = xs match { case (h::t) => if (x == h) { 0 } else { seek(x,t) + 1 } }
	
	
	private def renameType(t: Type, typecontext: List[TVar]): oil.Type = {
		t -> {
			case (x: TVar) => oil.TypeVar(seek(x, typecontext))
			case Top() => oil.Top()
			case Bot() => oil.Bot()
			case FunctionType(typeformals, argtypes, returntype) => {
				val newTypeContext = typeformals ::: typecontext
				val newArgTypes = argtypes map (renameType(_, newTypeContext))
				val newReturnType = renameType(returntype, newTypeContext)
				oil.ArrowType(typeformals.size, newArgTypes, newReturnType)
			}
			case _ => oil.Bot() 
			// Oil type spec is incomplete:
			
			// case NativeType(name) =>
			// case TupleType(elements) =>
			// case TypeApplication(tycon, typeactuals) =>
			// case AssertedType(assertedType) =>
		}
	}	
	
	private def renameDef(defn: Def, context: List[EVar], typecontext: List[TVar]): oil.Def = {
		defn -> {
			case Def(_, args, body, typeformals, argtypes, returntype) => {
				val newContext = args ::: context
				val newTypeContext = typeformals ::: typecontext 
				val newbody = rename(body, newContext, newTypeContext)
				val newArgTypes = argtypes map (renameType(_, newTypeContext))
				val newReturnType = returntype map (renameType(_, newTypeContext))
				oil.Def(typeformals.size, args.size, newbody, newArgTypes, newReturnType.get) // Fragile. Oil should have optional return types.
			}
		}
	}

	private def renameArgument(a: Argument, context: List[EVar], typecontext: List[TVar]): oil.Argument = {
		a -> {
            case Constant(s: orc.sites.Site) => oil.Constant(s)
            case Constant(v) => oil.Constant(oil.Literal(v))
			case (x: EVar) => oil.Variable(seek(x, context)) 
		}
	}
	
	private def rename(e: Expression, context: List[EVar], typecontext: List[TVar]): oil.Expression = {
		def renamer(e: Expression) = rename(e, context, typecontext)
		e -> {
			case Stop() => oil.Stop()
			case (a : Argument) => renameArgument(a, context, typecontext)
			case Call(target, args, typeargs) => {
				val newtarget = renameArgument(target, context, typecontext)
				val newargs = args map (renameArgument(_, context, typecontext))
				val newtypeargs = (typeargs.getOrElse(Nil)) map (renameType(_, typecontext)) // Fragile. Oil should have optional type args.
				oil.Call(newtarget, newargs, newtypeargs)
			}
			case Parallel(left, right) => oil.Parallel(renamer(left), renamer(right))
			case Sequence(left, x, right) => oil.Sequence(renamer(left), rename(right, x::context, typecontext))
			case Prune(left, x, right) => oil.Prune(rename(left, x::context, typecontext), renamer(right))
			case Otherwise(left, right) => oil.Otherwise(renamer(left), renamer(right))
			case DeclareDefs(defs, body) => {
				val defnames = defs map (_.f)
				val newdefs = defs map (renameDef(_, defnames ::: context, typecontext))
				val newbody = rename(body, defnames ::: context, typecontext)
				oil.DeclareDefs(newdefs, newbody)
			}
			case HasType(body, expectedType) => {
				oil.HasType(renamer(body), renameType(expectedType, typecontext))
			}
			
		}
		// don't forget to do topological sort as a post-pass here 
	}

	/*
     * Removes unused definitions from the OIL AST.
     */
	def remUnusedDefs(e: Expression): Expression = {
		e match {
			case Parallel(left, right) => Parallel(remUnusedDefs(left), remUnusedDefs(right))
			case Sequence(left, x, right) => Sequence(remUnusedDefs(left), x, remUnusedDefs(right))
			case Prune(left, x, right) => Prune(remUnusedDefs(left), x, remUnusedDefs(right))
			case Otherwise(left, right) => Otherwise(remUnusedDefs(left), remUnusedDefs(right))
			case DeclareDefs(defs, body) => {
				val newbody = remUnusedDefs(body)
				// If none of the defs are bound in the body,
	        	// just return the body.
	        	if(body.freevars -- defs.map(_.name) isEmpty) {
	        		newbody
	        	} else {
	        		def f(d: Def): Def = {
	        			d match { 
	        				case Def(name,args,body,t,a,r) => Def(name,args,remUnusedDefs(body),t,a,r)
	        			}
	        		}
	        		val newdefs = defs.map(f)
	        		DeclareDefs(newdefs, newbody)
	        	}
			}
			case HasType(body, typ) => HasType(remUnusedDefs(body), typ)
			case _ => e
		}
	}
	
	trait hasFreeVars {
	  val freevars: Set[EVar]
	}

	abstract class Expression extends AST with hasFreeVars {
		// Infix combinator constructors
		def ||(g: Expression) = Parallel(this,g)
		def >>(g: Expression) = Sequence(this,generateEVar,g)
		def <<(g: Expression) = Prune(this,generateEVar,g)
		def semicolon(g: Expression) = Otherwise(this,g)
		
		lazy val freevars:Set[EVar] = {
			this match {
			case v:EVar => Set(v)
			case Call(target,args,_) => target.freevars ++ args.flatMap(_.freevars)
			case Parallel(left,right) => left.freevars ++ right.freevars
			case Sequence(left,x,right) => left.freevars ++ (right.freevars - x)
			case Prune(left,x,right) => left.freevars ++ (right.freevars - x)
			case Otherwise(left,right) => left.freevars ++ right.freevars
			case DeclareDefs(defs,body) => (body.freevars ++ defs.flatMap(_.freevars)) -- defs.map(_.name)
			case HasType(body,typ) => body.freevars
			case _ => Set.empty
			}
		}
	}
		case class Stop extends Expression
		case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
		case class Parallel(left: Expression, right: Expression) extends Expression
		case class Sequence(left: Expression, x: EVar, right: Expression) extends Expression 
		case class Prune(left: Expression, x: EVar, right: Expression) extends Expression
		case class Otherwise(left: Expression, right: Expression) extends Expression
		case class DeclareDefs(defs : List[Def], body: Expression) extends Expression
		case class HasType(body: Expression, expectedType: Type) extends Expression
		abstract class Argument extends Expression
			class EVar extends Argument
			case class Constant(value: Any) extends Argument
	
	case class Def(f: EVar, args: List[EVar], body: Expression, typeformals: List[TVar], argtypes: List[Type], returntype: Option[Type]) extends AST with hasFreeVars {
			lazy val freevars: Set[EVar] = body.freevars -- args
			def name = f
		}
	
	abstract class Type extends AST
		case class TVar extends Type
		case class Top extends Type
		case class Bot extends Type
		case class NativeType(name: String) extends Type
		case class TupleType(elements: List[Type]) extends Type
		case class FunctionType(typeformals: List[TVar], argtypes: List[Type], returntype: Type) extends Type
		case class TypeApplication(tycon: TVar, typeactuals: List[Type]) extends Type
		case class AssertedType(assertedType: Type) extends Type
	

}}}
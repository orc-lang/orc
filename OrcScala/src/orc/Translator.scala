package orc.translation

import scala.collection.immutable._
import orc.PartialMapExtension._
import orc.oil.named._
import orc.oil._
import orc.ext
import orc.lib.builtin
import orc.sites.Site
import orc.sites.OrcSiteForm
import orc.sites.JavaSiteForm
import orc.OrcOptions

object Translator {
	
	
	def translate(options: OrcOptions, extendedAST : ext.Expression): Expression = {
		val namedAST = convert(extendedAST, new TreeMap(), new TreeMap())	
		namedAST map {
			case NamedVar(s) => throw new Exception("Unbound variable " + s)
			case y => y 
		}
		namedAST
	}
	
	def generateTempVar = new TempVar()
	def generateTempTypevar = new TempTypevar()
	
	
	
	
	
	
	
	def unfold(es: List[Expression], makeCore: List[Argument] => Expression): Expression = {
		
		def expand(es: List[Expression]): (List[Argument], Expression => Expression) = 
			es match {
				case (a : Argument) :: rest => {
					val (args, bindRest) = expand(rest)
					(a :: args, bindRest)
				}
				case g :: rest => {
					val (args, bindRest) = expand(rest)
					val x = generateTempVar
					(x :: args, bindRest(_) < x < g)
				}
				case Nil => (Nil, e => e)
			}
		
		val (args, bind) = expand(es)
		bind(makeCore(args))
	}
	
	def nullaryBuiltinCall(s : Site)() = Call(Constant(s), Nil, None)
	def unaryBuiltinCall(s : Site)(a : Argument) = Call(Constant(s), List(a), None)
	def binaryBuiltinCall(s : Site)(a : Argument, b: Argument) = Call(Constant(s), List(a, b), None)
	
	val callIf = unaryBuiltinCall(builtin.If) _
	val callNot = unaryBuiltinCall(builtin.Not) _
	val callEq = binaryBuiltinCall(builtin.Eq) _
	
	val callCons = binaryBuiltinCall(builtin.ConsConstructor) _
	val callIsCons = unaryBuiltinCall(builtin.ConsExtractor) _
	val callNil = nullaryBuiltinCall(builtin.NilConstructor) _
	val callIsNil = unaryBuiltinCall(builtin.NilExtractor) _
	
	val callSome = unaryBuiltinCall(builtin.SomeConstructor) _
	val callIsSome = unaryBuiltinCall(builtin.SomeExtractor) _
	val callNone = nullaryBuiltinCall(builtin.NoneConstructor) _
	val callIsNone = unaryBuiltinCall(builtin.NoneExtractor) _
	
	
	def makeUnapply(constructor : Argument, a : Argument) = {
		val extractor = generateTempVar
		val getExtractor = Call(Constant(builtin.FindExtractor), List(constructor), None)
		val invokeExtractor = Call(extractor, List(a), None)
		getExtractor > extractor > invokeExtractor
	}
	
	def makeNth(a : Argument, i : Int) = Call(a, List(Constant(Literal(i))), None)
	
	def makeLet(args: List[Argument]): Expression = {
		args match {
			case Nil => Constant(Literal({}))
			case List(a) => a
			case _ => makeTuple(args)
		}
	}
	
	def makeTuple(elements: List[Argument]) = Call(Constant(builtin.TupleConstructor), elements, None)
	
	def makeList(elements: List[Argument]) = {
		val nil: Expression = Call(Constant(builtin.NilConstructor), Nil, None)
		def cons(h: Argument, t: Expression): Expression = {
			val y = generateTempVar
			t > y > callCons(h, y)
		}
		elements.foldRight(nil)(cons)
	}
	
	// Incomplete.
	def makeOperator(opName : String) = builtin.Let
	
	
	
	
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
	type PatternDecomposition = (List[(Expression, TempVar)], List[(String,TempVar)])
	
	def convertPattern(p : ext.Pattern, x : TempVar, context : Map[String, TempVar], typecontext : Map[String, TempTypevar]) = {
	
		def decomposePattern(p : ext.Pattern, x: TempVar): PatternDecomposition = {
			p match {
				case ext.Wildcard() => (Nil, Nil)
				case ext.ConstantPattern(c) => {
					val testexpr = callEq(x, Constant(Literal(c)))
					val guard = (testexpr, generateTempVar)
					(List(guard), Nil)
				}
				case ext.VariablePattern(name) => {
					val binding = (name, x)
					(Nil, List(binding))
				}
				case ext.TuplePattern(ps) => {
					val vars = (for (_ <- ps) yield generateTempVar).toList
					val exprs = List.range(0, ps.length) map (makeNth(x, _))
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
					val y = generateTempVar
					val consCompute = (callIsCons(x), y)
					val (subCompute, subBindings) = decomposePattern(ext.TuplePattern(List(ph,pt)), y)
					(consCompute :: subCompute, subBindings)
				}
				case ext.CallPattern(name, args) => {
					val y = generateTempVar 
					val matchCompute = (makeUnapply(context(name), x), y)
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
					val guard = (testexpr, generateTempVar)
					(List(guard), Nil)
				}
				// Slightly inefficient but very easy to read
				case ext.TypedPattern(p, t) => {
					val y = generateTempVar
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
	
	
	
	def combinatorPattern(combinator: (Expression, TempVar, Expression) => Expression,
						  source: ext.Expression,
						  p : ext.Pattern,
						  target: ext.Expression, 
						  context : Map[String, TempVar], 
						  typecontext : Map[String, TempTypevar]): Expression = 
	{ 
		val sourceVar = generateTempVar
		val (compute, bindings) = convertPattern(p, sourceVar, context, typecontext)
		val fragments = (for ((_,z) <- bindings) yield z).toList.removeDuplicates
		
		val nil = makeLet(fragments)
		def cons(oneCompute: (Expression, TempVar), expr2: Expression) = {
			val (expr1, x) = oneCompute
			Sequence(expr1, x, expr2)
		}
		val convertedSource = convert(source, context, typecontext)
		val newCompute = (convertedSource, sourceVar)::compute
		val newsource = newCompute.foldRight(nil)(cons)
		
		val mappedVar = generateTempVar
		val mapping = Map() ++ (fragments map (x => (x,generateTempVar)))
		val newbindings = bindings map { case (name,z) => (name, mapping(z)) }
		
		val nil2 = convert(target, context ++ newbindings, typecontext)
		def cons2(indexedFragment: (TempVar, Int), expr: Expression) = {
			val (z, i) = indexedFragment
 			combinator(makeNth(mappedVar,i), mapping(z), expr)
		}
		val indexedFragments = fragments.zipWithIndex
		val newtarget = indexedFragments.foldRight(nil2)(cons2)
		 
		combinator(newsource, mappedVar, newtarget)
	}
	
	
	
	
	type Clause = (List[ext.Pattern], ext.Expression)
	type Sig = (Option[List[String]], Option[List[ext.Type]], Option[ext.Type])
	
	
	// Incomplete.
	def convertClause(context: Map[String, TempVar], typecontext: Map[String, TempTypevar])
	 				 (args: List[TempVar])
					 (clause: Clause, fallthrough: Expression): Expression = {
		
		val (ps, target) = clause
		
		val (groupedComputes, groupedBindings) = 
			List unzip { 
				for ((p,x) <- ps zip args) yield 
					convertPattern(p, x, context, typecontext)
			}
		
		val computes: List[(Expression, TempVar)] = groupedComputes.flatten
		val bindings: List[(String, TempVar)] = groupedBindings.flatten
		
		val fragments = (for ((_,z) <- bindings) yield z).toList.removeDuplicates -- args
		
		val nil = makeLet(fragments)
		def cons(oneCompute: (Expression, TempVar), expr2: Expression) = {
			val (expr1, x) = oneCompute
			Sequence(expr1, x, expr2)
		}
		val newsource = computes.foldRight(nil)(cons)
		
		val mappedVar = generateTempVar
		val mapping = Map() ++ (fragments map (x => (x,generateTempVar)))
		val newbindings = bindings map { case (name,z) => (name, mapping(z)) }
		
		val nil2 = convert(target, context ++ newbindings, typecontext)
		def cons2(indexedFragment: (TempVar, Int), expr: Expression) = {
			val (z, i) = indexedFragment
 			makeNth(mappedVar,i)  > mapping(z) >  expr
		}
		val indexedFragments = fragments.zipWithIndex
		
		val newtarget = indexedFragments.foldRight(nil2)(cons2)
		
		val y = generateTempVar
		val z = generateTempVar
		
		( 
		     (newsource  > y >  callSome(y)) 
		  ow ( callNone() ) 
		) > z > 
		(    
		     (callIsSome(z)      > mappedVar >     newtarget)
		  || (callIsNone(z)   > generateTempVar >  fallthrough)   
		)		
	}
	
	
	
	
	
	
	def convertClauses(clauses: List[Clause], args: List[TempVar], context: Map[String, TempVar], typecontext: Map[String, TempTypevar]): Expression = {
		val nil: Expression = Stop()
		val cons = { convertClause(context,typecontext)(args) _ }
		clauses.foldRight(nil)(cons)
	}
	
	
	def convertDefs(defs: List[ext.DefDeclaration], context: Map[String, TempVar], typecontext: Map[String, TempTypevar]): (List[Def], Map[String, TempVar]) = {
		
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
					case Some(clauses) => {
						clausesMap update (name, newclause::clauses)
					}
				}
				sigMap.get(name) match {
					case None => 
						sigMap update (name, (None, maybeArgTypes, maybeReturnType))
					case Some((typeFormals2, maybeArgTypes2, maybeReturnType2)) =>
						sigMap update (name, (typeFormals2, unify(maybeArgTypes, maybeArgTypes2), unify(maybeReturnType, maybeReturnType2)))
				}
			}
			
			case ext.DefCapsule(name, formals, body, maybeReturnType) => {
				val (newformals, maybeArgTypes) = formalsPartition(formals)
				val newclause = (newformals, new ext.Capsule(body))
				clausesMap.get(name) match {
					case None => 
						clausesMap update (name, List(newclause))
					case Some(clauses) => {
					  clausesMap update (name, newclause::clauses)
					}
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

    for (clkey <- clausesMap.keySet) {
      val cl = clausesMap(clkey)
      var existsCapsule = false
      var existsNotCapsule = false
      for (aclause <- cl) {
        aclause match {
          case (_, ext.Capsule(_)) => 
            if (existsNotCapsule) throw new Exception(clkey+" is not declared as capsule.") 
            else existsCapsule = true
          case _ => 
            if (existsCapsule) throw new Exception(clkey+" is already declared as capsule.")
            else existsNotCapsule = true
        }
      }
    }
		
		if (!(sigMap.keySet subsetOf clausesMap.keySet)) { throw new Exception("Unused function signature") } // Sanity check. TODO: Make more helpful.
		
		val newcontext = context ++ (for (name <- clausesMap.keys) yield (name, generateTempVar))
		
		val newdefs = 
			(for (name <- clausesMap.keys) yield {
				val (typeformals, argtypes, returntype) = 
					sigMap(name) match {
						case (_,None,_) => throw new Exception("Arg types not fully specified for function")
						case (Some(typeformals), Some(argtypes), returntype) => (typeformals, argtypes, returntype)
						case (None, Some(argtypes), returntype) => (Nil, argtypes, returntype)
					}
				
				val newtypecontext = typecontext ++ (for (name <- typeformals) yield (name, generateTempTypevar))
				val newtypeformals = typeformals map newtypecontext
				val newargtypes = argtypes map (convertType(_, newtypecontext))
				val newreturntype = returntype map (convertType(_, newtypecontext))
				
				val args = newargtypes map (_ => generateTempVar)
				val body = convertClauses(clausesMap(name), args, newcontext, newtypecontext)
				
				Def(newcontext(name), args, body, newtypeformals, newargtypes, newreturntype)
			}).toList
		
		(newdefs, newcontext)
	}
	
	
	
	
	
	
	def convertType(t : ext.Type, typecontext : Map[String, TempTypevar]): Type = {
		t -> {
			case ext.Top() => Top()
			case ext.Bot() => Bot()
			case ext.NativeType(s) => NativeType(s)
			case ext.TupleType(ts) => TupleType(ts map (convertType(_,typecontext)))
			case ext.TypeVariable(name) => typecontext.getOrElse(name, throw new Exception("Unbound type variable"))
			case ext.FunctionType(typeformals, argtypes, returntype) => {
				val newTypeContext = typecontext ++ (for (x <- typeformals) yield (x, generateTempTypevar))
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
	
	
	def makeNewBody(body: ext.Expression) : ext.Expression = {
    val (defs, g) = defPartition(body)
    if (defs.size == 0) {
      throw new Exception("A capsule must contain at least one def")
      // set the source location in the exception
    }
    var defNames: List[String] = List()
    for (d <- defs) {
      val name = d match {
        case ext.Def(n, _, _, _) => n
        case ext.DefCapsule(n, _, _, _) => n
        case _ => ""
      }
      if (name != "" && !defNames.contains(name)) defNames = name::defNames
    }
    
    var recordCall : ext.Call = new ext.Call(new ext.Constant(builtin.RecordConstructor), List(ext.Args(None, makeRecordArgs(defNames))))
    
    ext.Parallel(ext.Sequential(body, None, ext.Stop()), recordCall)
	}
	
	def makeRecordArgs(defNames: List[String]) : List[ext.Expression] = {
	  var args : List[ext.Expression] = List()
	  for (d <- defNames) {
	    args = args++List(new ext.Constant(d))
	    args = args++List(new ext.Call(new ext.Constant(builtin.SiteSite), List(ext.Args(None, List(new ext.Constant(d))))))
	  }
	  args
	} 

	
	// Incomplete for some cases.
	def convert(e : ext.Expression, context : Map[String, TempVar], typecontext : Map[String, TempTypevar]): Expression = {
			def converter(e : ext.Expression) = convert(e,context,typecontext)
			e -> {
				case ext.Stop() => Stop()
				case ext.Constant(c) => c match {
					case (v: Value) => Constant(v)
					case lit => Constant(Literal(lit))
				}
				case ext.Variable(x) => context(x)
				case ext.TupleExpr(es) => unfold(es map converter, makeLet)
				case ext.ListExpr(es) => unfold(es map converter, makeList)
                case ext.Call(target, List(ext.Args(typeargs, args))) => {
                	val newtypeargs = typeargs map { _ map { convertType(_, typecontext) } }
                	unfold(
                			          (target :: args) map converter, 
                            { case (newtarget :: newargs) => Call(newtarget, newargs, newtypeargs) }
                		  )
                }
                case ext.Call(target, gs) => throw new UnsupportedOperationException("converter not implemented for calls of form: "+e)
                //TODO: replace above with general: ext.Call(target, gs) => 
				case ext.PrefixOperator(op, arg) => {
					val opsite = Constant(makeOperator(op))
					unfold(List(arg) map converter, Call(opsite,_,None))
				}
				case ext.InfixOperator(l, op, r) => {
					val opsite = Constant(makeOperator(op))
					unfold(List(l,r) map converter, Call(opsite,_,None))
				}
				case ext.Sequential(l, None, r) => converter(l) > generateTempVar > converter(r)
				case ext.Sequential(l, Some(ext.VariablePattern(name)), r) => {
					val x = generateTempVar
					Sequence(converter(l), x, convert(r, context + ((name, x)), typecontext)) 
				}
				case ext.Sequential(l, Some(p), r) => {
					combinatorPattern(Sequence, l, p, r, context, typecontext)
				}
				case ext.Parallel(l,r) => converter(l) || converter(r)
				case ext.Pruning(l, None, r) => converter(l) < generateTempVar < converter(r)
				case ext.Pruning(l, Some(ext.VariablePattern(name)), r) => {
					val x = generateTempVar
					Sequence(convert(l, context + ((name, x)), typecontext), x, converter(r)) 
				}
				case ext.Pruning(l, Some(p), r) => {
					def combinator(source: Expression, x: TempVar, target: Expression) = Prune(target, x, source)
					combinatorPattern(combinator, r, p, l, context, typecontext)
				}
				case ext.Otherwise(l, r) => converter(l) ow converter(r)
				case ext.Lambda(typeformals: List[String], formals, returntype, body) => {
//  				val newTypeContext = typecontext ++ (for (x:String <- typeformals) yield (x, generateTempTypevar))
//  				val newTypeFormals = typeformals map newTypeContext
//          val (newformals, maybeArgTypes) = formalsPartition(formals)
//  				val newReturnType = convertType(returntype.getOrElse(orc.ext.Top()), newTypeContext)
//          val defname = generateTempVar
          val lambdaExtDef = ext.Def("", formals, body, returntype)
          
//          var defLambda = Def(defname, newformals, converter(body), newTypeFormals, maybeArgTypes, newReturnType)
          val (newdefs, newcontext) = convertDefs(List(lambdaExtDef), context, typecontext)
          val nameVar = 
          newdefs(0) match {
            case Def(nv, _, _, _, _, _) => nv
          }
          DeclareDefs(newdefs, nameVar)
				}
				case ext.Capsule(b) => {
				  var capThunk = ext.Lambda(None, null, None, makeNewBody(b))				  
				  converter(ext.Call(
				    ext.Call(ext.Constant(builtin.SiteSite), List(ext.Args(None, List(capThunk)))), null))
				}
				case ext.Conditional(ifE, thenE, elseE) => {
					 val t = generateTempVar
					 val f = generateTempVar
					 val ifexp = converter(ifE)
					 val thenbranch = callIf(t) > generateTempVar > converter(thenE)
					 val elsebranch = callIf(f) > generateTempVar > converter(elseE)
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
				case ext.Declare(ext.SiteImport(name, sitename), body) => {
				  val v = generateTempVar
				  new Prune(convert(body, context + ((name, v)), typecontext), v, Constant(OrcSiteForm.resolve(sitename)))
				}
				case ext.Declare(ext.ClassImport(name, classname), body) => {
				  val v = generateTempVar
				  new Prune(convert(body, context + ((name, v)), typecontext), v, Constant(JavaSiteForm.resolve(classname)))
				}
				
				case ext.Declare(ext.Include(_, decls), body) => converter((decls foldRight body)(ext.Declare)) //FIXME: Incorporate filename in source location information
				
				case ext.TypeAscription(body, t) => HasType(converter(body),              convertType(t, typecontext)  )
				case  ext.TypeAssertion(body, t) => HasType(converter(body), AssertedType(convertType(t, typecontext)) )
				
		}
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
	
	

}

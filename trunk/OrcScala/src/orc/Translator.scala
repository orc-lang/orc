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

import orc.translation.PrimitiveForms._

object Translator {
	
	/**
	 *  Translate an extended AST to a named OIL AST.
	 *
	 */
	def translate(options: OrcOptions, extendedAST : ext.Expression): Expression = {		
		convert(extendedAST) map {
			case NamedVar(s) => throw new Exception("Unbound variable " + s)
			case y => y 
		}
	}
	
	
	
	
	/**
	 *  Convert an extended AST expression to a named OIL expression.
	 *
	 */
	// FIXME: Incomplete for some cases.
	def convert(e : ext.Expression): named.Expression = {
			e -> {
				case ext.Stop => Stop
				case ext.Constant(c) => c match {
					case (v: Value) => Constant(v)
					case lit => Constant(Literal(lit))
				}
				case ext.Variable(x) => new NamedVar(x)
				case ext.TupleExpr(es) => unfold(es map convert, makeLet)
				case ext.ListExpr(es) => unfold(es map convert, makeList)
                case ext.Call(target, List(ext.Args(typeargs, args))) => {
                	val newtypeargs = typeargs map { _ map convertType }
                	unfold(
                			          (target :: args) map convert, 
                            { case (newtarget :: newargs) => Call(newtarget, newargs, newtypeargs) }
                		  )
                }
                case ext.Call(target, gs) => throw new UnsupportedOperationException("converter not implemented for calls of form: "+e)
                //FIXME: replace above with general: ext.Call(target, gs) => 
                
                case ext.PrefixOperator(op, exp) => {
                    val opName = if (op == "-") "0-" else op
                    unfold(List(exp) map convert, { callOperator(opName,_) })
                }
				case ext.InfixOperator(l, op, r) => {
					unfold(List(l,r) map convert, { callOperator(op,_) })
				}
				
				case ext.Sequential(l, None, r) => convert(l) >> convert(r)
				case ext.Sequential(l, Some(ext.VariablePattern(name)), r) => {
					val x = new TempVar()
					convert(l)  > x >  convert(r).subst(x, name)
				}
				case ext.Sequential(l, Some(p), r) => {
					val (filter, scope) = convertPattern(p)
					val x = new TempVar()
					filter(convert(l)) > x > scope(x)(convert(r))
				}
				
				case ext.Pruning(l, None, r) => convert(l) << convert(r)
				case ext.Pruning(l, Some(ext.VariablePattern(name)), r) => {
					val x = new TempVar()
					convert(l).subst(x, name)  < x <  convert(r) 
				}
				case ext.Pruning(l, Some(p), r) => {
					val (filter, scope) = convertPattern(p)
					val x = new TempVar()
					scope(x)(convert(l)) < x < filter(convert(r))
				}
				
				case ext.Parallel(l,r) => convert(l) || convert(r)
				case ext.Otherwise(l, r) => convert(l) ow convert(r)
				
				case lambda : ext.Lambda => {
					val lambdaName = new TempVar()
					val newdef = AggregateDef(lambda).convert(lambdaName)
					DeclareDefs(List(newdef), lambdaName)
				}
				case ext.Capsule(b) => {
				  var capThunk = ext.Lambda(None, null, None, makeNewBody(b))				  
				  convert(ext.Call(
				    ext.Call(ext.Constant(builtin.SiteSite), List(ext.Args(None, List(capThunk)))), null))
				}
				case ext.Conditional(ifE, thenE, elseE) => {
					 val b = new TempVar()
					 val nb = new TempVar()
					 (
					  (	  
						   callIf(b) >> convert(thenE) 
					   || callIf(nb) >> convert(elseE)	  
				      ) < nb < callNot(b) 
				     )   < b < convert(ifE)
				}
				case ext.Declare(decl : ext.DefDeclaration, _) => {
					val (defs, remainder) = e.defPartition
					val (newdefs, newcontext) = convertDefs(defs)
					DeclareDefs(newdefs, convert(remainder))
				}
				case ext.Declare(ext.Val(p,f), body) => {
					convert(ext.Pruning(body, Some(p), f))
				}
				
				/*FIXME:
				  case ext.Declare(_, body) =>
					case class TypeAlias(name: String, typeformals: List[String] = Nil, aliasedtype: Type) extends Declaration
					case class Datatype(name: String, typeformals: List[String] = Nil, constructors: List[Constructor]) extends Declaration
					case class Constructor(name: String, types: List[Option[Type]]) extends Declaration
					case class TypeImport(name: String, classname: String) extends Declaration
				*/
				//FIXME: Ignoring type imports
                case ext.Declare(ext.TypeImport(name, sitename), body) => { convert(body) }

				case ext.Declare(ext.SiteImport(name, sitename), body) => {
				  val site = Constant(OrcSiteForm.resolve(sitename))
				  convert(body).subst(site, name) 
				}
				case ext.Declare(ext.ClassImport(name, classname), body) => {
				  val site = Constant(JavaSiteForm.resolve(classname))
				  convert(body).subst(site, name)
				}
				
				//FIXME: Incorporate filename in source location information
				case ext.Declare(ext.Include(_, decls), body) => convert( (decls foldRight body)(ext.Declare) ) 
				
				case ext.TypeAscription(body, t) => HasType(convert(body), convertType(t))
				case ext.TypeAssertion(body, t) => HasType(convert(body), AssertedType(convertType(t)))
				
		}
	}
	
	
	/**
	 * Given (e1, ... , en) and f, return:
	 * 
	 * f(x1, ... , xn) <x1< e1 
	 *                  ... 
	 *                   <xn< en
	 * 
	 * As an optimization, if any e is already an argument, no << binder is generated for it.
	 * 
	 */
	def unfold(es: List[Expression], makeCore: List[Argument] => Expression): Expression = {
		
		def expand(es: List[Expression]): (List[Argument], Expression => Expression) = 
			es match {
				case (a : Argument) :: rest => {
					val (args, bindRest) = expand(rest)
					(a :: args, bindRest)
				}
				case g :: rest => {
					val (args, bindRest) = expand(rest)
					val x = new TempVar()
					(x :: args, bindRest(_) < x < g)
				}
				case Nil => (Nil, e => e)
			}
		
		val (args, bind) = expand(es)
		bind(makeCore(args))
	}
	
	
	
	/** 
	 * Helper functions for capsule conversion
	 */
	def makeNewBody(body: ext.Expression) : ext.Expression = {
	  val (defs, g) = body.defPartition
      if (defs.size == 0) {
        body !! "A capsule must contain at least one def"
      }
	  val defNames = (defs map { _.name }).removeDuplicates
      val recordCall : ext.Call = new ext.Call(new ext.Constant(builtin.RecordConstructor), List(ext.Args(None, makeRecordArgs(defNames))))
      ext.Parallel(ext.Sequential(body, None, ext.Stop), recordCall)
	}
	def makeRecordArgs(defNames: List[String]) : List[ext.Expression] = {
	  var args : List[ext.Expression] = List()
	  for (d <- defNames) {
	    args = args ::: List(new ext.Constant(d))
	    args = args ::: List(new ext.Call(new ext.Constant(builtin.SiteSite), List(ext.Args(None, List(new ext.Constant(d))))))
	  }
	  args
	}
	
	
	
	
	/**
	 *  Convert a list of extended AST def declarations to:
	 *  
	 *        a list of named OIL definitions 
	 *  and   
	 *        a function binding those definitions in a given scope
	 *
	 */
	def convertDefs(defs: List[ext.DefDeclaration]): (List[Def], Expression => Expression) = {
		import scala.collection.mutable
		val defsMap : mutable.Map[String, AggregateDef] = new mutable.HashMap()
		for (d <- defs) {
			val name = d.name
			val currentEntry = defsMap.get(name).getOrElse(AggregateDef.empty)
			val newEntry = currentEntry + d
			defsMap update (d.name, newEntry)
		}

		defsMap.values foreach { _.capsuleCheck }
		
		val namings = ( for (name <- defsMap.keys) yield (new TempVar(), name) ).toList
		val newdefs = for ((x, name) <- namings) yield {
			defsMap(name).convert(x).substAll(namings)
		}
		
		(newdefs, { _.substAll(namings) })
	}
	
	
	
	/**
	 *  Convert an extended AST type to a named OIL type.
	 *
	 */
	def convertType(t : ext.Type): named.Type = {
		t -> {
			case ext.TupleType(ts) => TupleType(ts map convertType)
			case ext.TypeVariable(name) => NamedTypevar(name)
			case ext.TypeApplication(name, typeactuals) => {
				TypeApplication(new NamedTypevar(name), typeactuals map convertType)
			}
			case ext.FunctionType(typeformals, argtypes, returntype) => {
				
				val substitutions = for (tf <- typeformals) yield (new TempTypevar(), tf)
				val doSubstitution : (Type, (Typevar,String)) => Type = { 
					case (t, (u,n)) => t.subst(u,n) 
				} 
				val doAllSubstitutions : Type => Type = { 
					substitutions.foldLeft(_)(doSubstitution) 
				}
				val convertAndSub = { convertType _ } andThen doAllSubstitutions
				
				val newTypeFormals = for ((u,_) <- substitutions) yield u
				val newArgTypes = argtypes map convertAndSub
				val newReturnType = convertAndSub(returntype)
				
				FunctionType(newTypeFormals, newArgTypes, newReturnType)
			}
			case ext.Top() => Top()
			case ext.Bot() => Bot()
		}
	}
	
	
	
	
	// FIXME: Incomplete
	
	/**
	 *  Convert an extended AST pattern to:
	 *  
	 *        A filter function applied to the source
	 *  and      
	 *        A binding function applied to the variable carrying the
	 *  	  result and then applied to the target
	 *
	 */
	def convertPattern(p : ext.Pattern): (Expression => Expression, TempVar => Expression => Expression) = {
	
		val x = new TempVar()
		val (computes, bindings) = decomposePattern(p, x) 
		
		// Check for nonlinearity.
		val (names, _) = List unzip bindings
		for (name <- names) {
			if (names exists (_ equals name)) {
				p !! ("Nonlinear pattern: " + name + " occurs more than once.")
			}
		}
		
		// Stub.
		(e => e, _ => e => e)
		
		/*def filter(source: Expression) = {
			var filterExpression =  
			
			source  > x >  filterExpr    
		}
	
		def scope(y : TempVar)(target : Expression) = {
			
		}
		
		(filter, scope)
		*/
	}
	


	/** 
	 * Decompose a pattern into two components:
	 * 
	 * 		A sequence of operations which extract intermediate values from the source expression.
	 * and
	 *  	A sequence of context bindings for the target expression.	
	 */
	type PatternDecomposition = (List[(Expression, TempVar)], List[(String,TempVar)])
	
	def decomposePattern(p : ext.Pattern, x: TempVar): PatternDecomposition = {
			p match {
				case ext.Wildcard => (Nil, Nil)
				case ext.ConstantPattern(c) => {
					val testexpr = callEq(x, Constant(Literal(c)))
					val guard = (testexpr, new TempVar())
					(List(guard), Nil)
				}
				case ext.VariablePattern(name) => {
					val binding = (name, x)
					(Nil, List(binding))
				}
				case ext.TuplePattern(ps) => {
					val vars = (for (_ <- ps) yield new TempVar()).toList
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
					val y = new TempVar()
					val consCompute = (callIsCons(x), y)
					val (subCompute, subBindings) = decomposePattern(ext.TuplePattern(List(ph,pt)), y)
					(consCompute :: subCompute, subBindings)
				}
				case ext.CallPattern(name, args) => {
					val y = new TempVar() 
					val matchCompute = (makeUnapply(new NamedVar(name), x), y)
					val (subCompute, subBindings) = decomposePattern(ext.TuplePattern(args), y)
					(matchCompute :: subCompute, subBindings)
				}
				case ext.AsPattern(p, name) => {
					val binding = (name, x)
					val (subCompute, subBindings) = decomposePattern(p, x)
					(subCompute, binding :: subBindings)
				}
				case ext.EqPattern(name) => {
					val testexpr = callEq(x, new NamedVar(name))
					val guard = (testexpr, new TempVar())
					(List(guard), Nil)
				}
				// Inefficient, but easier to read
				case ext.TypedPattern(p, t) => {
					val y = new TempVar()
					val typedCompute = (HasType(x, convertType(t)), y)
					val (subCompute, subBindings) = decomposePattern(p, y)
					(typedCompute :: subCompute, subBindings) 
				}			
			}
		}

	

}

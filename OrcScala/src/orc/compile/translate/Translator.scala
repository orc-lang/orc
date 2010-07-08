//
// Translator.scala -- Scala object Translator
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 27, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import scala.collection.immutable._
import orc.PartialMapExtension._
import orc.oil.named._
import orc.oil._
import orc.compile.ext
import orc.lib.builtin
import orc.values.sites.Site
import orc.values.sites.OrcSiteForm
import orc.values.sites.JavaSiteForm
import orc.OrcOptions
import orc.values.Field
import orc.values.Signal

import orc.compile.translate.PrimitiveForms._

object Translator {
	
	/**
	 *  Translate an extended AST to a named OIL AST.
	 *
	 */
	def translate(options: OrcOptions, extendedAST : ext.Expression): Expression = {		
		val namedAST = convert(extendedAST)
	    //Console.err.println("Translation result: \n" + namedAST)
		namedAST remapArgument {
		  case x@ NamedVar(s) => x !! ("Unbound variable " + s)
		  case a => a 
		}
		namedAST remapType {
		  case u@ NamedTypevar(s) => u !! ("Unbound type variable " + s)
		  case t => t
		}
		
	}
	
	
	
	
	/**
	 *  Convert an extended AST expression to a named OIL expression.
	 *
	 */
	def convert(e : ext.Expression): named.Expression = {
			e -> {
				case ext.Stop() => Stop()
				case ext.Constant(c) => Constant(c)
				case ext.Variable(x) => new NamedVar(x)
				case ext.TupleExpr(es) => {
				  if (es.size < 2) { 
				    e !! "Malformed tuple expression; a tuple must contain at least 2 elements" 
				  } 
				  unfold(es map convert, makeTuple)
				}
				case ext.ListExpr(es) => unfold(es map convert, makeList)
				case ext.RecordExpr(es) => {
				  val tuples = es map { case (s,e) => ext.TupleExpr(List(ext.Constant(s),e)) }
				  unfold(tuples map convert, makeRecord)
				}
                case ext.Call(target, gs) => {
                	var expr = convert(target)
                	for (g <- gs) {
                	  expr = unfold(List(expr), { case List(m) => callArgumentGroup(m, g) }) 
                	}
                	expr
                }
                case ext.PrefixOperator(op, exp) => {
                    val opName = if (op == "-") "0-" else op
                    unfold(List(exp) map convert, { callOperator(opName,_) })
                }
				case ext.InfixOperator(l, op, r) => {
					unfold(List(l,r) map convert, { callOperator(op,_) })
				}
				
				case ext.Sequential(l, None, r) => convert(l) >> convert(r)
				case ext.Sequential(l, Some(p), r) => {
					val (filter, scope) = convertPattern(p)
					val x = new TempVar()
					filter(convert(l)) > x > scope(x)(convert(r))
				}
				case ext.Pruning(l, None, r) => convert(l) << convert(r)
				case ext.Pruning(l, Some(p), r) => {
					val (filter, scope) = convertPattern(p)
					val x = new TempVar()
					scope(x)(convert(l)) < x < filter(convert(r))
				}
				case ext.Parallel(l,r) => convert(l) || convert(r)
				case ext.Otherwise(l, r) => convert(l) ow convert(r)
				
				case lambda : ext.Lambda => {
				    val flatLambda = reduceParamLists(lambda)
					val lambdaName = new TempVar()
					val newdef = AggregateDef(flatLambda).convert(lambdaName)
					DeclareDefs(List(newdef), lambdaName)
				}
				case ext.Capsule(b) => {
				  var capThunk = ext.Lambda(None, List(Nil), None, makeCapsuleBody(b))				  
				  convert(ext.Call(
				    ext.Call(ext.Constant(builtin.SiteSite), List(ext.Args(None, List(capThunk)))), List(ext.Args(None, Nil))))
				}
				case ext.Conditional(ifE, thenE, elseE) => {
					 val b = new TempVar()
					 val nb = new TempVar()
					 (  callIfT(b) >> convert(thenE) 
					 || callIfF(b) >> convert(elseE)	  
				     )   < b < convert(ifE)
				     
				}
				case ext.Declare(decl : ext.DefDeclaration, _) => {
					val (defs, remainder) = e.defPartition
					val (newdefs, scope) = convertDefs(defs)
					DeclareDefs(newdefs, scope(convert(remainder)))
				}
				case ext.Declare(ext.Val(p,f), body) => {
					convert(ext.Pruning(body, Some(p), f))
				}

				case ext.Declare(ext.SiteImport(name, sitename), body) => {
				  val site = Constant(OrcSiteForm.resolve(sitename))
				  convert(body).subst(site, name) 
				}
				case ext.Declare(ext.ClassImport(name, classname), body) => {
				  val u = new TempTypevar()
				  val site = Constant(JavaSiteForm.resolve(classname))
				  val newbody = convert(body).subst(site, name).substType(u, name)
				  DeclareType(u, ClassType(classname), newbody)
				}
				
				//FIXME: Incorporate filename in source location information
				case ext.Declare(ext.Include(_, decls), body) => convert( (decls foldRight body)(ext.Declare) ) 
				
				
				case ext.Declare(ext.TypeImport(name, classname), body) => {
				  val u = new TempTypevar()
                  val newbody = convert(body).substType(u, name)
                  DeclareType(u, ImportedType(classname), newbody)
				}
				
				case ext.Declare(ext.TypeAlias(name, typeformals, t), body) => {
				  val u = new TempTypevar()
                  val newbody = convert(body).substType(u, name)
                  val newtype = typeformals match {
                     case Nil => convertType(t)
                     case _ => {
                       val subs = for (tf <- typeformals) yield (new TempTypevar(), tf)
                       val newTypeFormals = for ((w,_) <- subs) yield w
                       TypeAbstraction(newTypeFormals, convertType(t).substAllTypes(subs))
                     }
                  }
				  DeclareType(u, newtype, newbody)
				}
				
				case ext.Declare(ext.Datatype(name, typeformals, constructors), body) => {
				  val d = new TempTypevar()
				  var newbody = convert(body)
				  
				  val cs = new TempVar()
				  for ((ext.Constructor(name, _), i) <- constructors.zipWithIndex) {
                      val x = new TempVar()
                      newbody = newbody.subst(x, name)
				      newbody = newbody < x < makeNth(cs,i)
                    }
				  newbody = newbody < cs < makeDatatype(d, constructors) 
				  
				  val variantType = { 
                    val subs = for (tf <- typeformals) yield (new TempTypevar(), tf)
                    val newTypeFormals = for ((w,_) <- subs) yield w
                    val variants = 
                      for (ext.Constructor(name, types) <- constructors) yield {
                        val newtypes = types map {_ map { convertType(_).substAllTypes(subs) } }
                        (name, newtypes)
                      }
                    TypeAbstraction(newTypeFormals, VariantType(variants))
                  }
				  newbody = newbody.substType(d, name)
				  newbody = DeclareType(d, variantType.substType(d, name), newbody)
				  
				  newbody
				}
                
				case ext.TypeAscription(body, t) => HasType(convert(body), convertType(t))
				case ext.TypeAssertion(body, t) => HasType(convert(body), AssertedType(convertType(t)))
				
		} setPos e.pos
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
	
	def callArgumentGroup(target: Argument, ag : ext.ArgumentGroup) : Expression = {
      ag match {
         case ext.Args(typeargs, args) => {
           val newtypeargs = typeargs map { _ map convertType }
           unfold(args map convert, { Call(target, _, newtypeargs) })
         }
         case ext.FieldAccess(field) => {
           Call(target, List(Constant(Field(field))), None)
         }
         case ext.Dereference => {
           val reader = new TempVar()
           Call(target, List(Constant(Field("read"))), None)  > reader >  Call(reader, Nil, None)
         }
      }
    }
	
	/** 
	 * Helper functions for capsule conversion
	 */
	def makeCapsuleBody(body: ext.Expression) : ext.Expression = makeCapsuleBody(body,Nil)

	def makeCapsuleBody(body: ext.Expression, defNames : List[String]) : ext.Expression = {
	  body match {
	    case ext.Declare(decl: ext.DefDeclaration, e) => {
	      return new ext.Declare(decl, makeCapsuleBody(e, decl.name :: defNames))
	    }
	    case ext.Declare(decl, e) => {
	      return new ext.Declare(decl, makeCapsuleBody(e, defNames))
	    }
	    case _ => {}
	  }
	  val dNames = defNames.distinct
      if (dNames.isEmpty) {
        body !! "A capsule must contain at least one def"
      }
      val recordCall : ext.Call = new ext.Call(new ext.Constant(builtin.RecordConstructor), List(ext.Args(None, makeRecordArgs(dNames))))
      ext.Parallel(ext.Sequential(body, None, ext.Stop()), recordCall)
	}
	
	/**
	 * Builds a list of Tuples (def-name,site-call) for every 
	 * definition name in the input list.
	 */
	def makeRecordArgs(defNames: List[String]) : List[ext.Expression] = {
      var args : List[ext.Expression] = Nil
      for (d <- defNames) {
        val call = new ext.Call(new ext.Constant(builtin.SiteSite), List(ext.Args(None, List(new ext.Variable(d)))))
        val tuple = ext.TupleExpr(List(new ext.Constant(d), call))
        args = args ::: List(tuple)
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

		val oneParamListDefs = defs map reduceParamLists
		
		val defsMap : mutable.Map[String, AggregateDef] = new mutable.HashMap()
		for (d <- oneParamListDefs) {
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
	 * Converts a definition 
	 * 
	 * def f(x1,..xn)(y1,..yn)(..) = body
	 * 
	 * to 
	 * 
	 * def f(x1,..xn) = lambda(y1,..yn) (lambda(...)..) body
	 * 
	 */
	 private def reduceParamLists(d: ext.DefDeclaration): ext.DefDeclaration = {
      import orc.error.compiletime.typing._
      d -> {
        case ext.Def(name,List(formals),retType,body) => d
        case ext.Def(name,formals::tail,retType,body) => {
          val newbody = uncurry(tail,body,retType)
          /* Return the outermost Def */
          ext.Def(name,List(formals),None,newbody) 
        }
        case ext.DefCapsule(name,formals,retType,body) => {
          reduceParamLists(ext.Def(name,formals,retType,ext.Capsule(body)))
        }
        case ext.DefSig(name,typFormals,List(argTypes),retType) => d
        case ext.DefSig(name,typFormals,argTypes::tail,retType) => {
          val lambdaType = ext.LambdaType(Nil,tail,retType)
          val newRetType = lambdaType.cut
          ext.DefSig(name,typFormals,List(argTypes),newRetType)
        }
        case ext.DefSig(_,_,List(),_) => 
          throw new UnspecifiedArgTypesException()
      }
	}
	
	/**
     * Given formals = (x1,..xn)(y1,..yn)(..)
     * builds the expression
     *   lambda(x1,..xn) = (lambda(y1,..yn) = (lambda(...) = .. body ))
     */
    private def uncurry(formals: List[List[ext.Pattern]], body: ext.Expression, retType: Option[ext.Type]): ext.Lambda = {
      
      def makeLambda(body: ext.Expression, params: List[ext.Pattern]) = 
        ext.Lambda(None,List(params),None,body)
     
      val revFormals = formals reverse
      /* Inner most lambda has the return type of the curried definition */
      val innerLambda = ext.Lambda(None,List(revFormals head),retType,body) 
      /* Make new Lambda expressions, one for each remaining list of formals */
      revFormals.tail.foldLeft(innerLambda)(makeLambda)
    }
     
    private def reduceParamLists(e: ext.Lambda): ext.Lambda = {
      e -> {
        case ext.Lambda(typFormals,List(formals),retType,body) => e
        case ext.Lambda(typFormals,formals::tail,retType,body) => {      
          val newbody = uncurry(tail,body,retType)
          ext.Lambda(typFormals,List(formals),None,newbody)  
        }
      }
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
			case ext.LambdaType(typeformals, List(argtypes), returntype) => {
				val subs = for (tf <- typeformals) yield (new TempTypevar(), tf)
				val newTypeFormals = for ((u,_) <- subs) yield u
				val newArgTypes = argtypes map { convertType(_).substAllTypes(subs) }
				val newReturnType = convertType(returntype).substAllTypes(subs)
				FunctionType(newTypeFormals, newArgTypes, newReturnType)
			}
			case ltype@ ext.LambdaType(typeformals, args::tail, returntype) => { 
			  /* Multiple type argument groups, first uncurry it.*/
			  convertType(ltype.cut)
			}
			case ext.Top() => Top()
			case ext.Bot() => Bot()
		}
	}
	
	
	
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
	
		val sourceVar = new TempVar()
		val (computes, bindings) = decomposePattern(p, sourceVar) 
		
		/* Check for nonlinearity */
		val (_, names) = bindings.unzip
		for (name <- names) {
			if ((names count {_ equals name}) > 1) {
				p !! ("Nonlinear pattern: " + name + " occurs more than once.")
			}
		}
		
		val neededResults = bindings.map({ case (y,_) => y }).distinct
				
				
		/* Create filter function */
		def filter(e : Expression) = {
		  computes match {
		     case Nil => e
		     case _ => {
		       var filterExpression = makeLet(neededResults)
		       for ((f,y) <- computes.reverse) {
		         filterExpression =  f  > y >  filterExpression
		       }
		       e > sourceVar > filterExpression
		     }
		  }
		}
				
		/* Create scope function */
		def scope(filterResult : TempVar)(e : Expression) = {
			val target = e.substAll(bindings)
			neededResults match {
			  case Nil => target
			  case y :: Nil => target.subst(filterResult,y)
			  case _ => {
			    var newtarget = target
			    for ((y, i) <- neededResults.zipWithIndex) {
                  val z = new TempVar()
                  newtarget = newtarget.subst(z,y)  < z <  makeNth(filterResult, i)
                }
			    newtarget
			  }
			}
		}
				
		(filter, scope)
		
	}
	


	/** 
	 * Decompose a pattern into two components:
	 * 
	 * 		A sequence of operations which extract intermediate values from the source expression.
	 * and
	 *  	A sequence of context bindings for the target expression.	
	 */
	type PatternDecomposition = (List[(Expression, TempVar)], List[(TempVar, String)])
	
	def decomposePattern(p : ext.Pattern, x: TempVar): PatternDecomposition = {
			p match {
				case ext.Wildcard() => (Nil, Nil)
				case ext.ConstantPattern(c) => {
					val b = new TempVar()
				    val testexpr = callEq(x, Constant(c)) > b > callIfT(b)
					val guard = (testexpr, new TempVar())
					(List(guard), Nil)
				}
				case ext.VariablePattern(name) => {
					val binding = (x, name)
					(Nil, List(binding))
				}
				case ext.TuplePattern(Nil) => decomposePattern(ext.ConstantPattern(Signal), x)
				case ext.TuplePattern(List(p)) => decomposePattern(p,x)
				case ext.TuplePattern(ps) => {
				    val vars = (for (_ <- ps) yield new TempVar()).toList
					val subResults = (ps, vars).zipped.map(decomposePattern)
					val (subComputeList, subBindingsList) = subResults.unzip
					val subComputes = subComputeList.flatten
					val subBindings = subBindingsList.flatten
					
					/* Test that the pattern's size matches the source tuple's size */
				    val testSizeExpr = callTupleArityChecker(x,Constant(BigInt(vars.size)))
					val testSize = (testSizeExpr,new TempVar())
					
					var computeElements: List[(Expression, TempVar)] = Nil
					for ((y, i) <- vars.zipWithIndex) {
					  computeElements = (makeNth(x,i), y) :: computeElements
					}					
					(testSize :: computeElements ::: subComputes, subBindings)
				}
				case ext.ListPattern(Nil) => {
				  val computeNil = (callIsNil(x),new TempVar())
				  (List(computeNil), Nil)
				}
				case ext.ListPattern(ps) => {
					val seed : ext.Pattern = ext.ListPattern(Nil)
					val folded = (ps foldRight seed)(ext.ConsPattern)	
					decomposePattern(folded, x)
				}
				case ext.ConsPattern(ph,pt) => {
					val y = new TempVar()
					val computeCons = (callIsCons(x), y)
					val (subComputes, subBindings) = decomposePattern(ext.TuplePattern(List(ph,pt)), y)
					(computeCons :: subComputes, subBindings)
				}
				case ext.CallPattern(name, args) => {
					val y = new TempVar() 
					val matchCompute = (makeUnapply(new NamedVar(name), x), y)
					val (subComputes, subBindings) = decomposePattern(ext.TuplePattern(args), y)
					(matchCompute :: subComputes, subBindings)
				}
				case ext.AsPattern(p, name) => {
					val binding = (x, name)
					val (subCompute, subBindings) = decomposePattern(p, x)
					(subCompute, binding :: subBindings)
				}
				case ext.EqPattern(name) => {
					val b = new TempVar()
				    val testexpr = callEq(x, new NamedVar(name)) > b > callIfT(b)
					val guard = (testexpr, new TempVar())
					(List(guard), Nil)
				}
				// TODO: Make this more efficient; the runtime compute is unnecessary.
				case ext.TypedPattern(p,t) => {
				  val y = new TempVar()
				  val typedCompute = (HasType(x, convertType(t)), y)
                  val (subCompute, subBindings) = decomposePattern(p, y)
                  (typedCompute :: subCompute, subBindings)
				}
			}
		}

	

}

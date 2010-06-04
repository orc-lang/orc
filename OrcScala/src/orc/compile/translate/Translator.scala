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
import orc.values.Value
import orc.values.Literal
import orc.values.Field

import orc.compile.translate.PrimitiveForms._

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
				case ext.Stop() => Stop()
				case ext.Constant(c) => c match {
					case (v: Value) => Constant(v)
					case lit => Constant(Literal(lit))
				}
				case ext.Variable(x) => new NamedVar(x)
				case ext.TupleExpr(es) => unfold(es map convert, makeLet)
				case ext.ListExpr(es) => unfold(es map convert, makeList)
                case ext.Call(target, gs) => {
                	var expr = convert(target)
                	for (g <- gs) {
                	  val m = new TempVar()
                	  expr = expr  > m >  callArgumentGroup(m, g) 
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
				    val flatLambda = reduceParamLists(lambda)
					val lambdaName = new TempVar()
					val newdef = AggregateDef(flatLambda).convert(lambdaName)
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
					 ( (  callIf(b) >> convert(thenE) 
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
	def makeNewBody(body: ext.Expression) : ext.Expression = {
	  val (defs, g) = body.defPartition
      if (defs.size == 0) {
        body !! "A capsule must contain at least one def"
      }
	  val defNames = (defs map { _.name }).removeDuplicates
      val recordCall : ext.Call = new ext.Call(new ext.Constant(builtin.RecordConstructor), List(ext.Args(None, makeRecordArgs(defNames))))
      ext.Parallel(ext.Sequential(body, None, ext.Stop()), recordCall)
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
        case ext.Def(name,List(formals),body,retType) => d
        case ext.Def(name,formals::tail,body,retType) => {
          val newbody = uncurry(tail,body,retType)
          /* Return the outermost Def */
          ext.Def(name,List(formals),newbody,None) 
        }
        case ext.DefCapsule(name,formals,body,retType) => {
          reduceParamLists(ext.Def(name,formals,ext.Capsule(body),retType))
        }
        case ext.DefSig(name,typFormals,List(argTypes),retType) => d
        case ext.DefSig(name,typFormals,argTypes::tail,Some(retType)) => {
          val newRetType = tail.foldRight(retType)({ ext.FunctionType(Nil,_,_) })
          ext.DefSig(name,typFormals,List(argTypes),Some(newRetType))
        }
        case ext.DefSig(_,_,List(),_) => 
          throw new UnspecifiedArgTypesException()
        case ext.DefSig(_,_,argTypes::tail,None) => 
          throw new UnspecifiedReturnTypeException()
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
		val (_, names) = List unzip bindings
		for (name <- names) {
			if (names exists (_ equals name)) {
				p !! ("Nonlinear pattern: " + name + " occurs more than once.")
			}
		}
		
		computes match {
			/* 
			 * There are no strict patterns. 
			 */
			case Nil => {
				({ e => e },  { x => { _.substAll(bindings).subst(x,sourceVar) } })
			}
			/* 
			 * There is exactly one strict pattern.
			 */
			case (e, y) :: Nil => {
				({ _  > sourceVar >  e},  { x => { _.substAll(bindings).subst(x,y) } })	
			}
			/*
			 * There are multiple strict patterns.
			 */
			case _ => { 
				val strictResults = computes map { case (_,y) => y }
				
				/* Create filter function */
				var filterExpression = makeLet(strictResults)
				for ((e,y) <- computes) {
					filterExpression =  e  > y >  filterExpression
				}
				def filter(e : Expression) = { e  > sourceVar >  filterExpression }
				
				/* Create scope function */
				def scope(filterResult : TempVar)(target : Expression) = {
					var newtarget = target.substAll(bindings)
					for ((y, i) <- strictResults.zipWithIndex) {
						val z = new TempVar()
						newtarget = newtarget.subst(z,y)  < z <  makeNth(filterResult, i)
					}
					newtarget
				}
				
				(filter, scope)
			}
		}  
		
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
					val testexpr = callEq(x, Constant(Literal(c)))
					val guard = (testexpr, new TempVar())
					(List(guard), Nil)
				}
				case ext.VariablePattern(name) => {
					val binding = (x, name)
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
					val binding = (x, name)
					val (subCompute, subBindings) = decomposePattern(p, x)
					(subCompute, binding :: subBindings)
				}
				case ext.EqPattern(name) => {
					val testexpr = callEq(x, new NamedVar(name))
					val guard = (testexpr, new TempVar())
					(List(guard), Nil)
				}
				// TODO: Reimplement correctly.
				case ext.TypedPattern(p,t) => decomposePattern(p,x) 			
			}
		}

	

}

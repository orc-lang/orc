//
// Named.scala -- Named representation of OIL syntax
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.oil.named

import orc.oil._
import orc.AST

// The supertype of all variable binding nodes
trait Scope
// The supertype of all type variable binding nodes
trait TypeScope

trait Var extends Argument
case class TempVar() extends Var
case class NamedVar(name : String) extends Var

trait Typevar extends Type
case class TempTypevar() extends Typevar
case class NamedTypevar(name : String) extends Typevar

trait hasFreeVars {
  val freevars: Set[Var]
}

sealed abstract class NamedAST() extends AST with NamedToNameless with MapOnArguments

sealed abstract class Expression() 
extends NamedAST with NamedInfixCombinators with hasFreeVars with RemoveUnusedDefinitions
{ 
  lazy val withoutNames: nameless.Expression = namedToNameless(this, Nil, Nil)
  def map(f: Argument => Argument): Expression = MapOnArgs(this, f)

  lazy val freevars:Set[Var] = {
    this match {
      case x:Var => Set(x)
      case Call(target,args,_) => target.freevars ++ args.flatMap(_.freevars)
      case left || right => left.freevars ++ right.freevars
      case left > x > right => left.freevars ++ (right.freevars - x)
      case left < x < right => left.freevars ++ (right.freevars - x)
      case left ow right => left.freevars ++ right.freevars
      case DeclareDefs(defs,body) => (body.freevars ++ defs.flatMap(_.freevars)) -- defs.map(_.name)
      case HasType(body,typ) => body.freevars
      case _ => Set.empty
    }
  }
}
case object Stop extends Expression
case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, x: TempVar, right: Expression) extends Expression with Scope
case class Prune(left: Expression, x: TempVar, right: Expression) extends Expression with Scope
case class Otherwise(left: Expression, right: Expression) extends Expression
case class DeclareDefs(defs : List[Def], body: Expression) extends Expression with Scope
case class HasType(body: Expression, expectedType: Type) extends Expression

sealed abstract class Argument() extends Expression
case class Constant(value: Value) extends Argument


sealed case class Def(name: TempVar, formals: List[TempVar], body: Expression, typeformals: List[TempTypevar], argtypes: List[Type], returntype: Option[Type]) 
extends NamedAST with Scope with TypeScope with hasFreeVars 
{ 
  lazy val withoutNames: nameless.Def = namedToNameless(this, Nil, Nil)
  lazy val freevars: Set[Var] = body.freevars -- formals
  def map(f: Argument => Argument): Def = MapOnArgs(this, f)

  // FIXME: Is equals correct here?
  def subst(a: Argument, x: Argument) = this map (y => if (y equals x) { a } else { y })
}


sealed abstract class Type() extends NamedAST
{ 
  lazy val withoutNames: nameless.Type = namedToNameless(this, Nil) 
  def map(f: Argument => Argument): Type = this
}	
case object Top extends Type
case object Bot extends Type
case class NativeType(name: String) extends Type
case class TupleType(elements: List[Type]) extends Type
case class TypeApplication(tycon: TempTypevar, typeactuals: List[Type]) extends Type
case class AssertedType(assertedType: Type) extends Type	
case class FunctionType(typeformals: List[TempTypevar], argtypes: List[Type], returntype: Type) 
extends Type with TypeScope


// Conversions from named to nameless representations
trait NamedToNameless {

  private def inverseLookup[A](x: A, xs: List[A]): Int = 
    xs match { case (h::t) => if (x == h) { 0 } else { inverseLookup(x,t) + 1 } }

  def namedToNameless(e: Expression, context: List[TempVar], typecontext: List[TempTypevar]): nameless.Expression = {
    def toExp(e: Expression): nameless.Expression = namedToNameless(e, context, typecontext)
    def toArg(a: Argument): nameless.Argument = namedToNameless(a, context)
    def toType(t: Type): nameless.Type = namedToNameless(t, typecontext)
    e -> {
      case Stop => nameless.Stop
      case a : Argument => namedToNameless(a, context)		
      case Call(target, args, typeargs) => nameless.Call(toArg(target), args map toArg, typeargs map { _ map toType })
      case left || right => nameless.Parallel(toExp(left), toExp(right))
      case left > x > right => nameless.Sequence(toExp(left), namedToNameless(right, x::context, typecontext))
      case left < x < right => nameless.Prune(namedToNameless(left, x::context, typecontext), toExp(right))
      case left ow right => nameless.Otherwise(toExp(left), toExp(right))
      case DeclareDefs(defs, body) => {
        val defnames = defs map { _.name }
        val newdefs = defs map { namedToNameless(_, defnames ::: context, typecontext) }
        val newbody = namedToNameless(body, defnames ::: context, typecontext)
        nameless.DeclareDefs(newdefs, newbody)
      }
      case HasType(body, expectedType) => nameless.HasType(toExp(body), toType(expectedType))
    } 
  }	

  def namedToNameless(a: Argument, context: List[TempVar]): nameless.Argument = {
    a -> {
      case Constant(v) => nameless.Constant(v)
      case (x: TempVar) => nameless.Variable(inverseLookup(x, context)) 
    }
  }


  def namedToNameless(t: Type, typecontext: List[TempTypevar]): nameless.Type = {
    def toType(t: Type): nameless.Type = namedToNameless(t, typecontext)
    t -> {
      case u: TempTypevar => nameless.TypeVar(inverseLookup(u, typecontext))
      case Top => nameless.Top
      case Bot => nameless.Bot
      case FunctionType(typeformals, argtypes, returntype) => {
        val newTypeContext = typeformals ::: typecontext
        val newArgTypes = argtypes map { namedToNameless(_, newTypeContext) }
        val newReturnType = namedToNameless(returntype, newTypeContext)
        nameless.FunctionType(typeformals.size, newArgTypes, newReturnType)
      }
      case NativeType(name) => nameless.NativeType(name)
      case TupleType(elements) => nameless.TupleType(elements map toType)
      case TypeApplication(tycon, typeactuals) => {
        val i = inverseLookup(tycon, typecontext)
        nameless.TypeApplication(i, typeactuals map toType)
      }	
      case AssertedType(assertedType) => nameless.AssertedType(namedToNameless(assertedType, typecontext))
    }
  }	

  def namedToNameless(defn: Def, context: List[TempVar], typecontext: List[TempTypevar]): nameless.Def = {
    defn -> {
      case Def(_, formals, body, typeformals, argtypes, returntype) => {
        val newContext = formals ::: context
        val newTypeContext = typeformals ::: typecontext 
        val newbody = namedToNameless(body, newContext, newTypeContext)
        val newArgTypes = argtypes map { namedToNameless(_, newTypeContext) }
        val newReturnType = returntype map { namedToNameless(_, newTypeContext) }
        nameless.Def(typeformals.size, formals.size, newbody, newArgTypes, newReturnType)
      }
    }
  }

}	


trait MapOnArguments { self: NamedAST =>
  
  def MapOnArgs(e: Expression, f: Argument => Argument): Expression = {
    def toExp(e: Expression): Expression = MapOnArgs(e, f)
    e -> {
      case Stop => Stop
      case a : Argument => f(a)	
      case Call(target, args, typeargs) => Call(f(target), args map f, typeargs)
      case left || right => toExp(left) || toExp(right)
      case left > x > right => toExp(left) > x > toExp(right)
      case left < x < right => toExp(left) < x < toExp(right)
      case left ow right => toExp(left) ow toExp(right)
      case DeclareDefs(defs, body) => DeclareDefs(defs map { MapOnArgs(_, f) }, toExp(body))
      case HasType(body, expectedType) => HasType(toExp(body), expectedType)
    }
  }	

  def MapOnArgs(defn: Def, f: Argument => Argument): Def = {
    defn -> {
      case Def(name, formals, body, typeformals, argtypes, returntype) => {
        Def(name, formals, MapOnArgs(body, f), typeformals, argtypes, returntype)
      }
    }
  }

}


trait RemoveUnusedDefinitions {
	self : Expression =>
	/*
     * Removes unused definitions from the OIL AST.
     */
	def removeUnusedDefs(): Expression = {
		this -> {
			case left || right => left.removeUnusedDefs() || right.removeUnusedDefs()
			case left > x > right => left.removeUnusedDefs() > x > right.removeUnusedDefs() 
			case left < x < right => left.removeUnusedDefs() < x < right.removeUnusedDefs()
			case left ow right => left.removeUnusedDefs() ow right.removeUnusedDefs()
			case DeclareDefs(defs, body) => {
				val newbody = body.removeUnusedDefs()
				// If none of the defs are bound in the body,
	        	// just return the body.
	        	if(body.freevars -- defs.map(_.name) isEmpty) {
	        		newbody
	        	} else {
	        		def f(d: Def): Def = {
	        			d match { 
	        				case Def(name,args,body,t,a,r) => Def(name,args,body.removeUnusedDefs(),t,a,r)
	        			}
	        		}
	        		val newdefs = defs.map(f)
	        		DeclareDefs(newdefs, newbody)
	        	}
			}
			case HasType(body, typ) => HasType(body.removeUnusedDefs(), typ)
			case _ => this
		}
	}
}


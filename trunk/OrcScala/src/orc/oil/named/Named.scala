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
import orc.values.Value
import orc.AST

// The supertype of all variable binding nodes
trait Scope
// The supertype of all type variable binding nodes
trait TypeScope


trait Var extends Argument
class TempVar(val optionalName : Option[String] = None) extends Var {
  def this(name: String) = this(Some(name))
}
case class NamedVar(name : String) extends Var

trait Typevar extends Type
class TempTypevar(val optionalName : Option[String] = None) extends Typevar {
  def this(name: String) = this(Some(name))
}
case class NamedTypevar(name : String) extends Typevar


trait hasFreeVars {
  val freevars: Set[Var]
}

sealed abstract class NamedAST extends AST with NamedToNameless {
  def prettyprint() = (new PrettyPrint()).reduce(this)
}

sealed abstract class Expression
extends NamedAST 
with NamedInfixCombinators 
with hasFreeVars 
with hasArgumentRemap[Expression]
with ArgumentSubstitution[Expression]
with hasTypeRemap[Expression]
with TypeSubstitution[Expression]
{ 
  lazy val withoutNames: nameless.Expression = namedToNameless(this, Nil, Nil)
  
  lazy val freevars:Set[Var] = {
    this match {
      case x:Var => Set(x)
      case Call(target,args,_) => target.freevars ++ args.flatMap(_.freevars)
      case left || right => left.freevars ++ right.freevars
      case left > x > right => left.freevars ++ (right.freevars - x)
      case left < x < right => (left.freevars - x) ++ right.freevars
      case left ow right => left.freevars ++ right.freevars
      case DeclareDefs(defs,body) => (body.freevars ++ defs.flatMap(_.freevars)) -- defs.map(_.name)
      case HasType(body,typ) => body.freevars
      case _ => Set.empty
    }
  }
  
  def remapArgument(f: Argument => Argument): Expression = 
    this -> {
      case Stop() => Stop()
      case a : Argument => f(a)	
      case Call(target, args, typeargs) => Call(f(target), args map f, typeargs)
      case left || right => (left remapArgument f) || (right remapArgument f)
      case left > x > right => (left remapArgument f) > x > (right remapArgument f)
      case left < x < right => (left remapArgument f) < x < (right remapArgument f)
      case left ow right => (left remapArgument f) ow (right remapArgument f)
      case DeclareDefs(defs, body) => DeclareDefs(defs map { _ remapArgument f }, body remapArgument f)
      case DeclareType(u, t, body) => DeclareType(u, t, body remapArgument f)
      case HasType(body, expectedType) => HasType(body remapArgument f, expectedType)
    } setPos pos
    
  def remapType(f: Typevar => Type): Expression = 
    this -> {
      case Stop() => Stop()
      case a : Argument => a
      case Call(target, args, typeargs) => Call(target, args, typeargs map { _ map { _ remapType f } })
      case left || right => (left remapType f) || (right remapType f)
      case left > x > right => (left remapType f) > x > (right remapType f)
      case left < x < right => (left remapType f) < x < (right remapType f)
      case left ow right => (left remapType f) ow (right remapType f)
      case DeclareDefs(defs, body) => DeclareDefs(defs map { _ remapType f }, body remapType f)
      case DeclareType(u, t, body) => DeclareType(u, t remapType f, body remapType f)
      case HasType(body, expectedType) => HasType(body remapType f, expectedType remapType f)
    } setPos pos
  
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

case class Stop() extends Expression
case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, x: TempVar, right: Expression) extends Expression with Scope
case class Prune(left: Expression, x: TempVar, right: Expression) extends Expression with Scope
case class Otherwise(left: Expression, right: Expression) extends Expression
case class DeclareDefs(defs : List[Def], body: Expression) extends Expression with Scope
case class DeclareType(name: TempTypevar, t: Type, body: Expression) extends Expression with TypeScope
case class HasType(body: Expression, expectedType: Type) extends Expression

sealed abstract class Argument extends Expression
case class Constant(value: Value) extends Argument


sealed case class Def(name: TempVar, formals: List[TempVar], body: Expression, typeformals: List[TempTypevar], argtypes: Option[List[Type]], returntype: Option[Type]) 
extends NamedAST 
with Scope 
with TypeScope 
with hasFreeVars 
with hasArgumentRemap[Def]
with ArgumentSubstitution[Def]
with hasTypeRemap[Def]
with TypeSubstitution[Def]
{ 
  lazy val withoutNames: nameless.Def = namedToNameless(this, Nil, Nil)
  lazy val freevars: Set[Var] = body.freevars -- formals
  
  def remapArgument(f: Argument => Argument): Def = {
    this ->> Def(name, formals, body remapArgument f, typeformals, argtypes, returntype)
  }

  def remapType(f: Typevar => Type): Def = {
    this ->> Def(name, formals, body remapType f, typeformals, argtypes map {_ map {_ remapType f} }, returntype map {_ remapType f})
  }

}


sealed abstract class Type extends NamedAST
with hasTypeRemap[Type]
with TypeSubstitution[Type]
{ 
  lazy val withoutNames: nameless.Type = namedToNameless(this, Nil) 
  
  def remapType(f: Typevar => Type): Type = 
	  this -> {
	 	  case x : Typevar => f(x)
	 	  case TupleType(elements) => TupleType(elements map {_ remapType f})
	 	  case TypeApplication(tycon, typeactuals) => {
	 	     f(tycon) match {
	 	       case (u : Typevar) => TypeApplication(u, typeactuals map {_ remapType f})
	 	       case other => this !! ("Erroneous type substitution; can't substitute " + other + " as a type constructor")
	 	     }
	 	     
	 	  }
	 	  case AssertedType(assertedType) => AssertedType(assertedType remapType f)
	 	  case FunctionType(typeformals, argtypes, returntype) =>
	 	  	FunctionType(typeformals, argtypes map {_ remapType f}, returntype remapType f)
	 	  case u => u
	  }
  
  def subst(t: Typevar, u: Typevar): Type = this remapType (y => if (y equals t) { u } else { y })   
  def subst(t: Typevar, s: String): Type = subst(t, NamedTypevar(s))
}	
case class Top() extends Type
case class Bot() extends Type
case class TupleType(elements: List[Type]) extends Type
case class TypeApplication(tycon: Typevar, typeactuals: List[Type]) extends Type
case class AssertedType(assertedType: Type) extends Type	
case class FunctionType(typeformals: List[TempTypevar], argtypes: List[Type], returntype: Type) extends Type with TypeScope
case class TypeAbstraction(typeformals: List[TempTypevar], t: Type) extends Type with TypeScope
case class ClassType(classname: String) extends Type
case class VariantType(variants: List[(String, List[Option[Type]])]) extends Type



// Conversions from named to nameless representations
trait NamedToNameless {

  def namedToNameless(e: Expression, context: List[TempVar], typecontext: List[TempTypevar]): nameless.Expression = {
    def toExp(e: Expression): nameless.Expression = namedToNameless(e, context, typecontext)
    def toArg(a: Argument): nameless.Argument = namedToNameless(a, context)
    def toType(t: Type): nameless.Type = namedToNameless(t, typecontext)
    e -> {
      case Stop() => nameless.Stop()
      case a : Argument => namedToNameless(a, context)		
      case Call(target, args, typeargs) => nameless.Call(toArg(target), args map toArg, typeargs map { _ map toType })
      case left || right => nameless.Parallel(toExp(left), toExp(right))
      case left > x > right => nameless.Sequence(toExp(left), namedToNameless(right, x::context, typecontext))
      case left < x < right => nameless.Prune(namedToNameless(left, x::context, typecontext), toExp(right))
      case left ow right => nameless.Otherwise(toExp(left), toExp(right))
      case DeclareDefs(defs, body) => {
        val defnames = defs map { _.name }
        val newdefs = defs map { namedToNameless(_, defnames.reverse ::: context, typecontext) }
        val newbody = namedToNameless(body, defnames ::: context, typecontext)
        nameless.DeclareDefs(newdefs, newbody)
      }
      case DeclareType(x, t, body) => {
        val newTypeContext = x::typecontext
        /* A type may be defined recursively, so its name is in scope for its own definition */
        val newt = namedToNameless(t, newTypeContext) 
        val newbody = namedToNameless(body, context, newTypeContext)
        nameless.DeclareType(newt, newbody)
      }
      case HasType(body, expectedType) => nameless.HasType(toExp(body), toType(expectedType))
    } setPos e.pos
  }	

  def namedToNameless(a: Argument, context: List[TempVar]): nameless.Argument = {
    a -> {
      case Constant(v) => nameless.Constant(v)
      case (x: TempVar) => nameless.Variable(context indexOf x) 
      case x@ NamedVar(s) => x !! ("Unbound variable " + s) 
    } setPos a.pos
  }


  def namedToNameless(t: Type, typecontext: List[TempTypevar]): nameless.Type = {
    def toType(t: Type): nameless.Type = namedToNameless(t, typecontext)
    t -> {
      case u: TempTypevar => nameless.TypeVar(typecontext indexOf u)
      case Top() => nameless.Top()
      case Bot() => nameless.Bot()
      case FunctionType(typeformals, argtypes, returntype) => {
        val newTypeContext = typeformals ::: typecontext
        val newArgTypes = argtypes map toType
        val newReturnType = namedToNameless(returntype, newTypeContext)
        nameless.FunctionType(typeformals.size, newArgTypes, newReturnType)
      }
      case TupleType(elements) => nameless.TupleType(elements map toType)
      case TypeApplication(tycon, typeactuals) => {
        val i = typecontext indexOf tycon
        nameless.TypeApplication(i, typeactuals map toType)
      }	
      case AssertedType(assertedType) => nameless.AssertedType(toType(assertedType))
      case TypeAbstraction(typeformals, t) => {
        val newTypeContext = typeformals ::: typecontext
        val newt = namedToNameless(t, newTypeContext)
        nameless.TypeAbstraction(typeformals.size, newt)
      }
      case ClassType(classname) => nameless.ClassType(classname)
      case VariantType(variants) => {
        val newVariants =
          for ((name, variant) <- variants) yield {
            (name, variant map {_ map toType})
          }
        nameless.VariantType(newVariants)
      }
      case u@ NamedTypevar(s) => u !! ("Unbound type variable " + s)
    } setPos t.pos
  }	

  def namedToNameless(defn: Def, context: List[TempVar], typecontext: List[TempTypevar]): nameless.Def = {
    defn -> {
      case Def(_, formals, body, typeformals, argtypes, returntype) => {
        val newContext = formals.reverse ::: context
        val newTypeContext = typeformals ::: typecontext 
        val newbody = namedToNameless(body, newContext, newTypeContext)
        val newArgTypes = argtypes map { _ map { namedToNameless(_, newTypeContext) } }
        val newReturnType = returntype map { namedToNameless(_, newTypeContext) }
        nameless.Def(typeformals.size, formals.size, newbody, newArgTypes, newReturnType)
      }
    } setPos defn.pos
  }

}	


trait hasArgumentRemap[X] {
  self : X =>
  
  def remapArgument(f : Argument => Argument): X
}

trait ArgumentSubstitution[X] extends NamedAST with hasArgumentRemap[X] {
  self : X =>
	
  def subst(a: Argument, x: Argument): X = 
	  this remapArgument (y => if (y equals x) { a } else { y })
  
  def substAllArgs(subs: List[(Argument, Argument)]): X = {
	  this remapArgument (y => {
	     val options = for ((a,x) <- subs if y equals x) yield a
	     options match {
	    	 case Nil => y
	    	 case List(a) => a
	    	 case _ => this !! ("Conflicting substitutions on " + y + ": " + options) 
	     }
	  })
  }
  
  
  def subst(a: Argument, s: String): X = subst(a, new NamedVar(s))
  
  def substAll(subs: List[(Argument, String)]): X = {
	  val newsubs = for ((a,s) <- subs) yield (a, new NamedVar(s))
	  substAllArgs(newsubs)
  }
  
  
	
}

trait hasTypeRemap[X] {
  self : X =>
  
  def remapType(f : Typevar => Type): X
}

trait TypeSubstitution[X] extends NamedAST with hasTypeRemap[X] {
  self : X =>
    
  def substType(t: Type, u: Typevar): X = 
      this remapType (y => if (y equals u) { t } else { y })
  
  def substTypes(subs: List[(Type, Typevar)]): X = {
      this remapType (y => {
         val options = for ((a,x) <- subs if y equals x) yield a
         options match {
             case Nil => y
             case List(a) => a
             case _ => this !! ("Conflicting substitutions on " + y + ": " + options) 
         }
      })
  }
  
  def substType(t: Type, s: String): X = substType(t, new NamedTypevar(s))
  
  def substAllTypes(subs: List[(Type, String)]): X = {
      val newsubs = for ((t,s) <- subs) yield (t, new NamedTypevar(s))
      substTypes(newsubs)
  }
  
  
    
}


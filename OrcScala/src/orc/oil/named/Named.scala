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

trait Var extends Argument
case class UnboundVar(name : String) extends Var
class BoundVar(val optionalName : Option[String] = None) extends Var {
  def this(name: String) = this(Some(name))
}


trait Typevar extends Type
case class UnboundTypevar(name : String) extends Typevar
class BoundTypevar(val optionalName : Option[String] = None) extends Typevar {
  def this(name: String) = this(Some(name))
}


// The supertype of all variable binding nodes
sealed trait Scope
// The supertype of all type variable binding nodes
sealed trait TypeScope


trait hasFreeVars {
  /* Note: As is evident from the type, UnboundVars are not included in this set */
  val freevars: Set[BoundVar]
}

trait hasFreeTypeVars {
  /* Note: As is evident from the type, UnboundTypevars are not included in this set */
  val freetypevars: Set[BoundTypevar]
}

sealed abstract class NamedAST extends AST with NamedToNameless {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()
  
  
  override val subtrees: List[NamedAST] = this match {
    case Call(target, args, typeargs) => target :: ( args ::: typeargs.toList.flatten )
    case left || right => List(left, right)
    case Sequence(left,x,right) => List(left, x, right)
    case Prune(left,x,right) => List(left, x, right)
    case left ow right => List(left, right)
    case DeclareDefs(defs, body) => defs ::: List(body)
    case HasType(body, expectedType) => List(body, expectedType)
    case DeclareType(u, t, body) => List(u, t, body)
    case Def(f, formals, body, typeformals, argtypes, returntype) => {
      f :: ( formals ::: ( List(body) ::: typeformals ::: argtypes.toList.flatten ::: returntype.toList ) )
    }
    case TupleType(elements) => elements
    case TypeApplication(tycon, typeactuals) => tycon :: typeactuals
    case AssertedType(assertedType) => List(assertedType)
    case TypeAbstraction(typeformals, t) => typeformals ::: List(t)
    case VariantType(variants) => {
      for ((_, variant) <- variants; Some(t) <- variant) yield t
    }
    case _ => Nil
  }
  
}

sealed abstract class Expression
extends NamedAST 
with NamedInfixCombinators 
with hasFreeVars 
with hasFreeTypeVars
with hasArgumentRemap[Expression]
with ArgumentSubstitution[Expression]
with hasTypeRemap[Expression]
with TypeSubstitution[Expression]
{ 
  lazy val withoutNames: nameless.Expression = namedToNameless(this, Nil, Nil)
  
  lazy val freevars:Set[BoundVar] = {
    this match {
      case x:BoundVar => Set(x)
      case Call(target,args,_) => target.freevars ++ (args flatMap { _.freevars })
      case left || right => left.freevars ++ right.freevars
      case left > x > right => left.freevars ++ (right.freevars - x)
      case left < x < right => (left.freevars - x) ++ right.freevars
      case left ow right => left.freevars ++ right.freevars
      case DeclareDefs(defs,body) => {
        val defSet = defs.toSet
        val defFreeVars = defSet flatMap {_.freevars}
        val defNameVars = defSet map {_.name }
        (body.freevars ++ defFreeVars) -- defNameVars
      }
      case HasType(body, _) => body.freevars
      case DeclareType(_, _, body) => body.freevars
      case _ => Set.empty
    }
  }
  
  lazy val freetypevars:Set[BoundTypevar] = {
    this match {
      case Call(target,args,ts) => target.freetypevars ++ (args flatMap { _.freetypevars }) ++ (ts.toList.flatten flatMap { _.freetypevars })
      case left || right => left.freetypevars ++ right.freetypevars
      case left > x > right => left.freetypevars ++ right.freetypevars
      case left < x < right => left.freetypevars ++ right.freetypevars
      case left ow right => left.freetypevars ++ right.freetypevars
      case DeclareDefs(defs,body) => {
        body.freetypevars ++ ( defs flatMap {_.freetypevars} )
      }
      case HasType(body, t) => body.freetypevars ++ t.freetypevars
      case DeclareType(u, t, body) => (body.freetypevars ++ t.freetypevars) - u
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
    }
    
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
    }
  
    
    
    
    
    lazy val withoutUnusedTypes: Expression = {
          this -> {
            case Stop() => this
            case (_ : Argument) => this 
            case Call(_,_,_) => this
            case left || right => left.withoutUnusedTypes || right.withoutUnusedTypes
            case left > x > right => left.withoutUnusedTypes > x > right.withoutUnusedTypes
            case left < x < right => left.withoutUnusedTypes < x < right.withoutUnusedTypes
            case left ow right => left.withoutUnusedTypes ow right.withoutUnusedTypes
            case DeclareDefs(defs, body) => {
              val newbody = body.withoutUnusedTypes
              val newdefs = defs map {
                case Def(name,args,body,t,a,r) => Def(name,args,body.withoutUnusedTypes,t,a,r)
              }
              DeclareDefs(newdefs, newbody)
            }
            case HasType(body, typ) => HasType(body.withoutUnusedTypes, typ)
            case DeclareType(u, t, body) => {
              val newbody = body.withoutUnusedTypes
              if (newbody.freetypevars contains u) {
                DeclareType(u, t, newbody)
              }
              else {
                newbody
              }
            }
          }
    }
    
    
    
    /*
     * Removes unused definitions from the OIL AST.
     * 
     * This is more useful on an AST with separated defs.
     */
	lazy val withoutUnusedDefs: Expression = {
		  this -> {
		    case Stop() => this
            case (_ : Argument) => this 
            case Call(_,_,_) => this
			case left || right => left.withoutUnusedDefs || right.withoutUnusedDefs
			case left > x > right => left.withoutUnusedDefs > x > right.withoutUnusedDefs 
			case left < x < right => left.withoutUnusedDefs < x < right.withoutUnusedDefs
			case left ow right => left.withoutUnusedDefs ow right.withoutUnusedDefs
			case DeclareDefs(defs, body) => {
				val newbody = body.withoutUnusedDefs
				// If none of the defs are bound in the body,
	        	// just return the body.
	        	val defNamesSet: Set[BoundVar] = defs.toSet map ((a:Def) => a.name)
                if(newbody.freevars intersect defNamesSet isEmpty) {
                  newbody
	        	} else {
	        		val newdefs = defs map {
	        		  case Def(name,args,body,t,a,r) => Def(name,args,body.withoutUnusedDefs,t,a,r)
	        		}
	        		DeclareDefs(newdefs, newbody)
	        	}
			}
			case HasType(body, typ) => HasType(body.withoutUnusedDefs, typ)
			case DeclareType(u, t, body) => DeclareType(u, t, body.withoutUnusedDefs)
		  }
	}
	
    lazy val fractionDefs: Expression = {
          this -> {
            case Stop() => this
            case (_ : Argument) => this 
            case Call(_,_,_) => this
            case left || right => left.fractionDefs || right.fractionDefs
            case left > x > right => left.fractionDefs > x > right.fractionDefs 
            case left < x < right => left.fractionDefs < x < right.fractionDefs
            case left ow right => left.fractionDefs ow right.fractionDefs
            case DeclareDefs(defs, body) => {
                val defslists = DefFractioner.fraction(defs)
                val newbody = body.fractionDefs
                defslists.foldRight(newbody) { DeclareDefs }
            }
            case HasType(body, typ) => {
              val newb = body.fractionDefs
              HasType(newb, typ)
            }
            case DeclareType(u, t, body) => { 
              val newb = body.fractionDefs 
              DeclareType(u, t, newb)
            }
          }
    }
    
}

case class Stop() extends Expression
case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, x: BoundVar, right: Expression) extends Expression with Scope
case class Prune(left: Expression, x: BoundVar, right: Expression) extends Expression with Scope
case class Otherwise(left: Expression, right: Expression) extends Expression
case class DeclareDefs(defs : List[Def], body: Expression) extends Expression with Scope
case class DeclareType(name: BoundTypevar, t: Type, body: Expression) extends Expression with TypeScope
case class HasType(body: Expression, expectedType: Type) extends Expression

sealed abstract class Argument extends Expression
case class Constant(value: AnyRef) extends Argument


sealed case class Def(name: BoundVar, formals: List[BoundVar], body: Expression, typeformals: List[BoundTypevar], argtypes: Option[List[Type]], returntype: Option[Type]) 
extends NamedAST 
with Scope 
with TypeScope 
with hasFreeVars 
with hasFreeTypeVars
with hasArgumentRemap[Def]
with ArgumentSubstitution[Def]
with hasTypeRemap[Def]
with TypeSubstitution[Def]
{ 
  lazy val withoutNames: nameless.Def = namedToNameless(this, Nil, Nil)
  lazy val freevars: Set[BoundVar] = body.freevars -- formals
  
  lazy val freetypevars: Set[BoundTypevar] = {
    val argvars = argtypes.toList.flatten flatMap { _.freetypevars }
    val retvars = returntype.toList flatMap { _.freetypevars }
    val bodyvars = body.freetypevars
    (argvars ++ retvars ++ bodyvars).toSet -- typeformals
  }
  
  def remapArgument(f: Argument => Argument): Def = {
    this ->> Def(name, formals, body remapArgument f, typeformals, argtypes, returntype)
  }

  def remapType(f: Typevar => Type): Def = {
    this ->> Def(name, formals, body remapType f, typeformals, argtypes map {_ map {_ remapType f} }, returntype map {_ remapType f})
  }

}


sealed abstract class Type extends NamedAST
with hasFreeTypeVars
with hasTypeRemap[Type]
with TypeSubstitution[Type]
{ 
  lazy val withoutNames: nameless.Type = namedToNameless(this, Nil) 
  
  lazy val freetypevars: Set[BoundTypevar] = 
    this match {
      case u : BoundTypevar => {
        Set(u)
      }
      case TupleType(elements) => {
        ( elements flatMap { _.freetypevars } ).toSet
      }
      case RecordType(entries) => { 
        ( entries.values flatMap { _.freetypevars } ).toSet
      }
      case AssertedType(assertedType) => {
        assertedType.freetypevars
      }
      case FunctionType(typeformals, argtypes, returntype) => {
        val argvars = ( argtypes flatMap { _.freetypevars } ).toSet 
        val retvars = ( returntype.freetypevars ).toSet
        argvars ++ retvars -- typeformals
      }
      case TypeAbstraction(typeformals, t) => {
        t.freetypevars -- typeformals
      }
      case TypeApplication(tycon : BoundTypevar, ts) => {
        val vars = ts flatMap { _.freetypevars }
        vars.toSet + tycon
      }
      case VariantType(variants) => {
        val ts = for ((_, variant) <- variants; Some(t) <- variant) yield t
        val vars = ts flatMap { _.freetypevars }
        vars.toSet
      }
      case _ => Set.empty
    }
  
  def remapType(f: Typevar => Type): Type = 
	  this -> {
	      case Bot() => Bot()
	      case Top() => Top()
	      case ImportedType(cl) => ImportedType(cl)
	      case ClassType(cl) => ClassType(cl)
	 	  case x : Typevar => f(x)
	 	  case TupleType(elements) => TupleType(elements map {_ remapType f})
	 	  case RecordType(entries) => {
	 	     val newEntries = entries map { case (s,t) => (s, t remapType f) }
	 	     RecordType(newEntries)
	 	  }
	 	  case TypeApplication(tycon, typeactuals) => {
	 	     f(tycon) match {
	 	       case (u : Typevar) => TypeApplication(u, typeactuals map {_ remapType f})
	 	       case other => this !! ("Erroneous type substitution; can't substitute " + other + " as a type constructor")
	 	     }
	 	  }
	 	  case AssertedType(assertedType) => AssertedType(assertedType remapType f)
	 	  case FunctionType(typeformals, argtypes, returntype) =>
	 	  	FunctionType(typeformals, argtypes map {_ remapType f}, returntype remapType f)
	 	  case TypeAbstraction(typeformals, t) => {
	 	    TypeAbstraction(typeformals, t remapType f)
	 	  }
	 	  case VariantType(variants) => {
	 	    val newVariants =
	 	      for ((name, variant) <- variants) yield {
	 	        (name, variant map { _ map { _ remapType f } })
	 	      }
	 	    VariantType(newVariants)
          }
	  }
  
  def subst(t: Typevar, u: Typevar): Type = this remapType (y => if (y equals t) { u } else { y })   
  def subst(t: Typevar, s: String): Type = subst(t, UnboundTypevar(s))
}	
case class Top() extends Type
case class Bot() extends Type
case class TupleType(elements: List[Type]) extends Type
case class RecordType(entries: Map[String,Type]) extends Type
case class TypeApplication(tycon: Typevar, typeactuals: List[Type]) extends Type
case class AssertedType(assertedType: Type) extends Type	
case class FunctionType(typeformals: List[BoundTypevar], argtypes: List[Type], returntype: Type) extends Type with TypeScope
case class TypeAbstraction(typeformals: List[BoundTypevar], t: Type) extends Type with TypeScope
case class ImportedType(classname: String) extends Type
case class ClassType(classname: String) extends Type
case class VariantType(variants: List[(String, List[Option[Type]])]) extends Type


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
  
  
  def subst(a: Argument, s: String): X = subst(a, new UnboundVar(s))
  
  def substAll(subs: List[(Argument, String)]): X = {
	  val newsubs = for ((a,s) <- subs) yield (a, new UnboundVar(s))
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
  
  def substType(t: Type, s: String): X = substType(t, new UnboundTypevar(s))
  
  def substAllTypes(subs: List[(Type, String)]): X = {
      val newsubs = for ((t,s) <- subs) yield (t, new UnboundTypevar(s))
      substTypes(newsubs)
  }
  
  
    
}

    

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

import scala.collection.mutable.LinkedList

import orc.oil._
import orc.values.Value
import orc.AST

// The supertype of all variable binding nodes
sealed trait Scope
// The supertype of all type variable binding nodes
sealed trait TypeScope


trait Var extends Argument
class BoundVar(val optionalName : Option[String] = None) extends Var {
  def this(name: String) = this(Some(name))
}
case class UnboundVar(name : String) extends Var

trait Typevar extends Type
class BoundTypevar(val optionalName : Option[String] = None) extends Typevar {
  def this(name: String) = this(Some(name))
}
case class UnboundTypevar(name : String) extends Typevar


trait hasFreeVars {
  /* Note: As is evident from the type, UnboundVars are not included in this set */
  val freevars: Set[BoundVar]
}

trait hasFreeTypeVars {
  /* Note: As is evident from the type, NamedTypevars are not included in this set */
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



// Conversions from named to nameless representations
trait NamedToNameless {

  def namedToNameless(e: Expression, context: List[BoundVar], typecontext: List[BoundTypevar]): nameless.Expression = {
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
        val opennames = (defs flatMap { _.freevars }).distinct filterNot { defnames contains _ }
        val defcontext = defnames.reverse ::: opennames ::: context
        val bodycontext = defnames.reverse ::: context
        val newdefs = defs map { namedToNameless(_, defcontext, typecontext) }
        val newbody = namedToNameless(body, bodycontext, typecontext)
        val openvars = 
          opennames map { x =>
            val i = context indexOf x
            if (i < 0) { e !! "Compiler fault: unbound closure variable in deBruijn conversion"+newbody }
            i
          }
        nameless.DeclareDefs(openvars, newdefs, newbody)
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

  def namedToNameless(a: Argument, context: List[BoundVar]): nameless.Argument = {
    a -> {
      case Constant(v) => nameless.Constant(v)
      case (x: BoundVar) => {
        var i = context indexOf x
        if (i < 0) { a !! "Compiler fault: unbound variable in deBruijn conversion" } 
        nameless.Variable(i) 
      }
      case x@ UnboundVar(s) => x !! ("Unbound variable " + s) 
    } setPos a.pos
  }


  def namedToNameless(t: Type, typecontext: List[BoundTypevar]): nameless.Type = {
    def toType(t: Type): nameless.Type = namedToNameless(t, typecontext)
    t -> {
      case u: BoundTypevar => {
        var i = typecontext indexOf u
        if (i < 0) { t !! "Compiler fault: unbound type variable in deBruijn conversion" } 
        nameless.TypeVar(i)
      }
      case Top() => nameless.Top()
      case Bot() => nameless.Bot()
      case FunctionType(typeformals, argtypes, returntype) => {
        val newTypeContext = typeformals ::: typecontext
        val newArgTypes = argtypes map toType
        val newReturnType = namedToNameless(returntype, newTypeContext)
        nameless.FunctionType(typeformals.size, newArgTypes, newReturnType)
      }
      case TupleType(elements) => nameless.TupleType(elements map toType)
      case RecordType(entries) => {
        val newEntries = entries map { case (s,t) => (s, toType(t)) }
        nameless.RecordType(newEntries)
      }
      case TypeApplication(tycon, typeactuals) => {
        val i = typecontext indexOf tycon
        if (i < 0) { t !! "Compiler fault: unbound type constructor in deBruijn conversion" } 
        nameless.TypeApplication(i, typeactuals map toType)
      }	
      case AssertedType(assertedType) => nameless.AssertedType(toType(assertedType))
      case TypeAbstraction(typeformals, t) => {
        val newTypeContext = typeformals ::: typecontext
        val newt = namedToNameless(t, newTypeContext)
        nameless.TypeAbstraction(typeformals.size, newt)
      }
      case ImportedType(classname) => nameless.ImportedType(classname)
      case ClassType(classname) => nameless.ClassType(classname)
      case VariantType(variants) => {
        val newVariants =
          for ((name, variant) <- variants) yield {
            (name, variant map {_ map toType})
          }
        nameless.VariantType(newVariants)
      }
      case u@ UnboundTypevar(s) => u !! ("Unbound type variable " + s)
    } setPos t.pos
  }	

  def namedToNameless(defn: Def, context: List[BoundVar], typecontext: List[BoundTypevar]): nameless.Def = {
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

object DefFractioner {
    /**
     * Divides a list of defs into a list of mutually recursive (sub)lists
     * of defs. The return list is ordered such that no mutually recursive
     * sub-list has references to definitions in the mutually recursive
     * sub-lists that follow it. 
     */
    def fraction(decls: List[Def]): LinkedList[List[Def]] = {
      if(decls.size == 1)
         return new LinkedList(decls,LinkedList.empty)
         
      val nodes = for(d <- decls) yield new Node(d)
      val g = new Graph(nodes)
       
      // Add edges in the graph.
      for{ n1 <- g.nodes 
           n2 <- g.nodes
           if (n1 != n2)
         } {
         val def1 = n1.elem 
         val def2 = n2.elem
         if (def1.freevars contains def2.name) {
           // Add Def (its node) points to other Defs that it refers to
           g.addEdge(n1,n2)
         }
      }

      /* Do a DFS on the graph, ignoring the resulting forest*/
      g.depthSearch(Direction.Forward)
      /* Sort the elements of the graph in decreasing order
       * of their finish times. */
      g.sort 
      /* Do a second DFS, on the complement of the original graph
       * (i.e, do a backward DFS). The result is a topologically
       * sorted collection of mutually recursive definitions */ 
      val forest:LinkedList[List[Node[Def]]] = g.depthSearch(Direction.Backward)
      // Extract the Defs from the Nodes.
      forest map {
         case l: List[Node[Def]] => l map {(n: Node[Def]) => n.elem}
      }
    }
}
    
 class Graph[T](var nodes: List[Node[T]]) {
      
     def addEdge(from: Node[T], to: Node[T]) {
        from.succs = to :: from.succs
        to.precs = from :: to.precs
      }

     def depthSearch(dir: Direction.Value): LinkedList[List[Node[T]]] = {
       var time = 0
       
       def search(node: Node[T], tree: List[Node[T]] ): List[Node[T]] = {
         var resultTree = node :: tree
         time = time + 1
         node.startTime = Some(time)
         var nextNodes = if (dir == Direction.Forward) { node.succs } else { node.precs }
         for (next <- nextNodes) {
           next.startTime match {
             case None => { resultTree = search(next,resultTree) }
             case Some(i) => {}
           }
         }
         time = time + 1
         node.finishTime = Some(time)
         resultTree
       }
       
       clear
       var forest: LinkedList[List[Node[T]]] = LinkedList.empty
       for (n <- nodes) {
         n.startTime match {
           case None => { forest = new LinkedList(search(n,Nil),forest) }
           case Some(i) => {}
         }
       }
       forest
     }
     
     def sort {
       nodes = nodes sortWith { (n1: Node[T], n2: Node[T]) => 
         (n1.finishTime ,n2.finishTime ) match {
           case (Some(i), Some(j)) => i > j
           case _ => false // !! Shd not occur.
         }
       }
     }
     
     def clear {
       for(n <- nodes) {
           n.startTime = None
           n.finishTime = None
       }
     }
    }

    object Direction extends Enumeration {
         type Direction = Value
         val Forward, Backward = Value
    }
    
    class Node[T](val elem: T) {
        var startTime:  Option[Int] = None   // Start time of the DFS for this node
        var finishTime: Option[Int] = None   // End time of the DFS for this node
        var succs:      List[Node[T]] = Nil
        var precs:      List[Node[T]] = Nil
    }

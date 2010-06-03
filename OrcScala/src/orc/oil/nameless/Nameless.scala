//
// Nameless.scala -- Nameless representation of OIL syntax
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

package orc.oil.nameless

import orc.oil._


trait hasFreeVars {
  val freevars: Set[Int]

  /* Reduce this set of indices by n levels. */
  def shift(indices: Set[Int], n: Int): Set[Int] =
    Set.empty ++ (for (i <- indices if i >= n) yield i-n)  
}


sealed abstract class Expression() extends orc.AST with hasFreeVars with NamelessInfixCombinators {

  /* 
   * Find the set of free vars for any given expression.
   * Inefficient, but very easy to read, and this is only computed once per node. 
   */
  lazy val freevars: Set[Int] = {
    this match {
      case Stop => Set.empty
      case Constant(_) => Set.empty
      case Variable(i) => Set(i)
      case Call(target, args, typeArgs) => target.freevars ++ args.flatMap(_.freevars)  
      case f || g => f.freevars ++ g.freevars
      case f >> g => f.freevars ++ shift(g.freevars, 1)
      case f << g => shift(f.freevars, 1) ++ g.freevars
      case f ow g => f.freevars ++ g.freevars
      case DeclareDefs(defs, body) => {
        /* Get the free vars, then bind the definition names */
        def f(x: hasFreeVars) = shift(x.freevars, defs.length)
        f(body) ++ defs.flatMap(f)
      }
      case HasType(body,_) => body.freevars
    }
  }
  
  lazy val withNames: named.Expression = AddNames.namelessToNamed(this, Nil, Nil)
}
case object Stop extends Expression
case class Call(target: Argument, args: List[Argument], typeArgs: Option[List[Type]]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, right: Expression) extends Expression
case class Prune(left: Expression, right: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression
case class DeclareDefs(defs: List[Def], body: Expression) extends Expression {
  /* A sorted list of the declaration's free variables. */
  lazy val freeVarList: List[Int] = freevars.toList.sortWith((x:Int,y:Int) => x < y)
}
case class HasType(body: Expression, expectedType: Type) extends Expression

sealed abstract class Argument() extends Expression
case class Constant(value: Value) extends Argument
case class Variable(index: Int) extends Argument

sealed abstract class Type() extends orc.AST
case class Top extends Type
case class Bot extends Type
case class TypeVar(index: Int) extends Type
case class TupleType(elements: List[Type]) extends Type
case class TypeApplication(tycon: Int, typeactuals: List[Type]) extends Type
case class AssertedType(assertedType: Type) extends Type	
case class FunctionType(typeFormalArity: Int, argTypes: List[Type], returnType: Type) extends Type

sealed case class Def(typeFormalArity: Int, arity: Int, body: Expression, argTypes: List[Type], returnType: Option[Type]) extends orc.AST with hasFreeVars {
  /* Get the free vars of the body, then bind the arguments */
  lazy val freevars: Set[Int] = shift(body.freevars, arity)
  /* Body with renamed free variable for closure compaction.
   * This is the actual expression called at runtime. */
  var altBody : Expression = body
}


// Conversions from nameless to named representations
object AddNames {

  import orc.oil.named.TempVar
  import orc.oil.named.TempTypevar
  import scala.Range

  def namelessToNamed(e: Expression, context: List[named.TempVar], typecontext: List[named.TempTypevar]): named.Expression = {
    def recurse(e: Expression): named.Expression = namelessToNamed(e, context, typecontext)
    e -> {
      case Stop => named.Stop
      case a: Argument => namelessToNamed(a, context)		
      case Call(target, args, typeargs) => {
        val newtarget = namelessToNamed(target, context)
        val newargs = args map { namelessToNamed(_, context) }
        val newtypeargs = typeargs map { _ map { namelessToNamed(_, typecontext) } }
        named.Call(newtarget, newargs, newtypeargs)
      }
      case left || right => named.Parallel(recurse(left), recurse(right))
      case left >> right => {
        val x = new TempVar()
        named.Sequence(recurse(left), x, namelessToNamed(right, x::context, typecontext))
      }
      case left << right => {
        val x = new TempVar()
        named.Prune(namelessToNamed(left, x::context, typecontext), x, recurse(right))
      }
      case left ow right => named.Otherwise(recurse(left), recurse(right))
      case DeclareDefs(defs, body) => {
        val defnames = defs map { _ => new TempVar() }
        val newcontext = defnames ::: context
        val newdefs = for ( (x,d) <- defnames zip defs) yield namelessToNamed(x, d, newcontext, typecontext)
        val newbody = namelessToNamed(body, newcontext, typecontext)
        named.DeclareDefs(newdefs, newbody)
      }
      case HasType(body, expectedType) => {
        named.HasType(recurse(body), namelessToNamed(expectedType, typecontext))
      }
    } 
  }	

  def namelessToNamed(a: Argument, context: List[TempVar]): named.Argument =
    a -> {
      case Constant(v) => named.Constant(v)
      case Variable(i) => context(i) 
    }

  def namelessToNamed(t: Type, typecontext: List[TempTypevar]): named.Type = {
    t -> {
      case TypeVar(i) => typecontext(i)
      case Top() => named.Top()
      case Bot() => named.Bot()
      case FunctionType(typearity, argtypes, returntype) => {
        val typeformals = (for (_ <- 0 until typearity) yield new TempTypevar()).toList
        val newTypeContext = typeformals ::: typecontext
        val newArgTypes = argtypes map { namelessToNamed(_, newTypeContext) }
        val newReturnType = namelessToNamed(returntype, newTypeContext)
        named.FunctionType(typeformals, newArgTypes, newReturnType)
      } 
      case TupleType(elements) => named.TupleType(elements map { namelessToNamed(_, typecontext) })
      case TypeApplication(i, typeactuals) => {
        val tycon = typecontext(i)
        val newTypeActuals = typeactuals map { namelessToNamed(_, typecontext) }
        named.TypeApplication(tycon, newTypeActuals)
      }
      case AssertedType(assertedType) => named.AssertedType(namelessToNamed(assertedType, typecontext))
    }
  }	

  def namelessToNamed(x: TempVar, defn: Def, context: List[TempVar], typecontext: List[TempTypevar]): named.Def = {
    defn -> {
      case Def(typearity, arity, body, argtypes, returntype) => {
        val formals = (for (_ <- 0 until arity) yield new TempVar()).toList
        val typeformals = (for (_ <- 0 until typearity) yield new TempTypevar()).toList
        val newContext = formals ::: context
        val newTypeContext = typeformals ::: typecontext 
        val newbody = namelessToNamed(body, newContext, newTypeContext)
        val newArgTypes = argtypes map { namelessToNamed(_, newTypeContext) }
        val newReturnType = returntype map  { namelessToNamed(_, newTypeContext) }
        named.Def(x, formals, newbody, typeformals, newArgTypes, newReturnType)
      }
    }
  }

}

//
// Oil.scala -- Scala object Oil
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.oil


abstract class Value
case class Literal(value: Any) extends Value

	// Abstract syntax: expressions, definitions, arguments

case class Stop extends Expression
case class Call(target: Argument, args: List[Argument], typeArgs: List[Type]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, right: Expression) extends Expression
case class Prune(left: Expression, right: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression
case class DeclareDefs(defs : List[Def], body: Expression) extends Expression
case class HasType(body: Expression, expectedType: Type) extends Expression

abstract class Argument extends Expression
case class Constant(value: Value) extends Argument
case class Variable(index: Int) extends Argument

abstract class Type extends orc.AST
case class Top extends Type
case class Bot extends Type
case class ArrowType(typeFormalArity: Int, argTypes: List[Type], returnType: Type) extends Type
case class TypeVar(index: Int) extends Type

case class Def(typeFormalArity: Int, arity: Int, body: Expression, argTypes : List[Type], returnType : Type) extends orc.AST with hasFreeVars {
	/* Get the free vars of the body, then bind the arguments */
	lazy val freevars: Set[Int] = shift(body.freevars, arity)
}


trait hasFreeVars {
	val freevars: Set[Int]

	/* Reduce this set of indices by n levels. */
	def shift(indices: Set[Int], n: Int) : Set[Int] =
	    Set.empty ++ (for (i <- indices if i >= n) yield i-n)  
}



abstract class Expression extends orc.AST with hasFreeVars {

	/* 
	 * Find the set of free vars for any given expression.
	 * Inefficient, but very easy to read, and this is only computed once per node. 
	 */
	lazy val freevars: Set[Int] = {
		this match {
		case Stop() => Set.empty
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
	
	// Infix combinator constructors
	def ||(g: Expression) = Parallel(this,g)
	def >>(g: Expression) = Sequence(this,g)
	def <<(g: Expression) =    Prune(this,g)
	def ow(g: Expression) =  Otherwise(this,g)

	
}

// Infix combinator extractors
object || {
	def apply(f: Expression, g: Expression) = Parallel(f,g)
	def unapply(e: Expression) =
		e match {
			case Parallel(l,r) => Some((l,r))
			case _ => None
		}
}

object >> {
	def unapply(e: Expression) =
		e match {
			case Sequence(l,r) => Some((l,r))
			case _ => None
		}
}

object << {
	def unapply(e: Expression) =
		e match {
			case Prune(l,r) => Some((l,r))
			case _ => None
		}
}

object ow {
	def unapply(e: Expression) =
		e match {
			case Otherwise(l,r) => Some((l,r))
			case _ => None
		}
}

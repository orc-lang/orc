//
// Nameless.scala -- Nameless representation of OIL syntax
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.nameless

import orc.ast.oil._
import orc.ast.AST
import orc.ast.hasOptionalVariableName

trait hasFreeVars {
  val freevars: Set[Int]

  /* Reduce this set of indices by n levels. */
  def shift(indices: Set[Int], n: Int): Set[Int] =
    Set.empty ++ (for (i <- indices if i >= n) yield i - n)
}

sealed abstract class NamelessAST extends AST {

  override val subtrees: Iterable[NamelessAST] = this match {
    case Call(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case left || right => List(left, right)
    case Sequence(left, right) => List(left, right)
    case LateBind(left, right) => List(left, right)
    case Limit(f) => List(f)
    case left ow right => List(left, right)
    case DeclareDefs(_, defs, body) => defs ::: List(body)
    case VtimeZone(timeOrder, body) => List(timeOrder, body)
    case HasType(body, expectedType) => List(body, expectedType)
    case DeclareType(t, body) => List(t, body)
    case Def(_, _, body, argtypes, returntype) => {
      body :: (argtypes.toList.flatten ::: returntype.toList)
    }
    case TupleType(elements) => elements
    case FunctionType(_, argTypes, returnType) => argTypes :+ returnType
    case TypeApplication(_, typeactuals) => typeactuals
    case AssertedType(assertedType) => List(assertedType)
    case TypeAbstraction(_, t) => List(t)
    case RecordType(entries) => entries.values
    case VariantType(_, variants) => {
      for ((_, variant) <- variants; t <- variant) yield t
    }
    case Constant(_) | UnboundVariable(_) | Variable(_) | Hole(_, _) | Stop() => Nil
    case Bot() | ClassType(_) | ImportedType(_) | Top() | TypeVar(_) | UnboundTypeVariable(_) => Nil
    case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in NamelessAST.subtrees")
  }
}

sealed abstract class Expression extends NamelessAST
  with hasFreeVars
  with NamelessInfixCombinators
  with NamelessToNamed {

  /*
   * Find the set of free vars for any given expression.
   * Inefficient, but very easy to read, and this is only computed once per node.
   */
  lazy val freevars: Set[Int] = {
    this match {
      case Stop() => Set.empty
      case Constant(_) => Set.empty
      case Variable(i) => Set(i)
      case Call(target, args, typeArgs) => target.freevars ++ (args flatMap { _.freevars })
      case f || g => f.freevars ++ g.freevars
      case f >> g => f.freevars ++ shift(g.freevars, 1)
      case f <<| g => shift(f.freevars, 1) ++ g.freevars
      case Limit(f) => f.freevars
      case f ow g => f.freevars ++ g.freevars
      case DeclareDefs(openvars, defs, body) => openvars.toSet ++ shift(body.freevars, defs.length)
      case HasType(body, _) => body.freevars
      case DeclareType(_, body) => body.freevars
      case VtimeZone(timeOrder, body) => timeOrder.freevars ++ body.freevars
      // Free variable determination will probably be incorrect on an expression with holes
      case Hole(_, _) => {
        Set.empty
      }
    }
  }

  /** Substitute values for variables in a nameless expression.
    * The context ctx is a stack of optional bindings.
    * The binding is Some(v) if the variable at that depth is to be replaced by v.
    * The binding is None if the variable at that depth is to remain unchanged.
    */
  def subst(ctx: List[Option[AnyRef]]): Expression = {
    this -> {
      case Stop() => Stop()
      case Constant(v) => Constant(v)
      case Variable(i) =>
        if (i < ctx.size) {
          ctx(i) match {
            case Some(v) => Constant(v)
            case None => Variable(i)
          }
        } else {
          Variable(i)
        }
      case Call(target, args, typeArgs) => {
        Call(target.subst(ctx).asInstanceOf[Argument], args map { _.subst(ctx).asInstanceOf[Argument] }, typeArgs)
      }
      case f || g => f.subst(ctx) || g.subst(ctx)
      case f >> g => f.subst(ctx) >> g.subst(None :: ctx)
      case f <<| g => f.subst(None :: ctx) <<| g.subst(ctx)
      case Limit(f) => Limit(f.subst(ctx))
      case f ow g => f.subst(ctx) ow g.subst(ctx)
      case DeclareDefs(openvars, defs, body) => {
        val newctx = (for (_ <- defs) yield None).toList ::: ctx
        val newdefs = {
          val defctx = (for (_ <- openvars) yield None).toList ::: newctx
          defs map { _.subst(defctx) }
        }
        val newbody = body.subst(newctx)
        DeclareDefs(openvars, newdefs, newbody)
      }
      case HasType(body, t) => HasType(body.subst(ctx), t)
      case DeclareType(t, body) => DeclareType(t, body.subst(ctx))
      case VtimeZone(timeOrder, body) => VtimeZone(timeOrder.subst(ctx).asInstanceOf[Argument], body.subst(ctx))
      case Hole(holeContext, holeTypeContext) => {
        Hole(holeContext mapValues { _.subst(ctx).asInstanceOf[Argument] }, holeTypeContext)
      }
    }
  }

  lazy val withNames: named.Expression = namelessToNamed(this, Nil, Nil)

  def prettyprint() = this.withNames.prettyprint()
}
case class Stop() extends Expression
case class Call(target: Argument, args: List[Argument], typeArgs: Option[List[Type]]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, right: Expression) extends Expression with hasOptionalVariableName
case class LateBind(left: Expression, right: Expression) extends Expression with hasOptionalVariableName
case class Limit(f: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression
case class DeclareDefs(unclosedVars: List[Int], defs: List[Def], body: Expression) extends Expression
case class DeclareType(t: Type, body: Expression) extends Expression with hasOptionalVariableName
case class HasType(body: Expression, expectedType: Type) extends Expression
case class Hole(context: Map[String, Argument], typecontext: Map[String, Type]) extends Expression
case class VtimeZone(timeOrder: Argument, body: Expression) extends Expression

sealed abstract class Argument extends Expression
case class Constant(value: AnyRef) extends Argument
case class Variable(index: Int) extends Argument with hasOptionalVariableName {
  require(index >= 0)
}
case class UnboundVariable(name: String) extends Argument with hasOptionalVariableName {
  optionalVariableName = Some(name)
}

sealed abstract class Type extends NamelessAST with NamelessToNamed {
  lazy val withNames: named.Type = namelessToNamed(this, Nil)
}
case class Top() extends Type
case class Bot() extends Type
case class TypeVar(index: Int) extends Type with hasOptionalVariableName
case class TupleType(elements: List[Type]) extends Type
case class RecordType(entries: Map[String, Type]) extends Type
case class TypeApplication(tycon: Int, typeactuals: List[Type]) extends Type
case class AssertedType(assertedType: Type) extends Type
case class FunctionType(typeFormalArity: Int, argTypes: List[Type], returnType: Type) extends Type
case class TypeAbstraction(typeFormalArity: Int, t: Type) extends Type
case class ImportedType(classname: String) extends Type
case class ClassType(classname: String) extends Type
case class VariantType(typeFormalArity: Int, variants: List[(String, List[Type])]) extends Type
case class UnboundTypeVariable(name: String) extends Type with hasOptionalVariableName {
  optionalVariableName = Some(name)
}

sealed case class Def(typeFormalArity: Int, arity: Int, body: Expression, argTypes: Option[List[Type]], returnType: Option[Type]) extends NamelessAST
  with hasFreeVars
  with hasOptionalVariableName {
  /* Get the free vars of the body, then bind the arguments */
  lazy val freevars: Set[Int] = shift(body.freevars, arity)

  def subst(ctx: List[Option[AnyRef]]): Def = {
    val newctx = (for (_ <- List.range(0, arity)) yield None).toList ::: ctx
    Def(typeFormalArity, arity, body.subst(newctx), argTypes, returnType)
  }

}

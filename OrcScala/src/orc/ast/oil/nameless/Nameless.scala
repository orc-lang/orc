//
// Nameless.scala -- Nameless representation of OIL syntax
// Project OrcScala
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.nameless

import orc.ast.oil.named
import orc.ast.{ AST, hasOptionalVariableName }

sealed trait hasFreeVars {
  val freevars: Set[Int]

  /* Reduce this set of indices by n levels. */
  def shift(indices: Set[Int], n: Int): Set[Int] =
    Set.empty ++ (for (i <- indices if i >= n) yield i - n)
}

sealed abstract class NamelessAST extends AST {

  override val subtrees: Iterable[NamelessAST] = this match {
    case Call(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case Parallel(left, right) => List(left, right)
    case Sequence(left, right) => List(left, right)
    case LateBind(left, right) => List(left, right)
    case Limit(f) => List(f)
    case Otherwise(left, right) => List(left, right)
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
      case UnboundVariable(_) => Set.empty // freevars does not include UnboundVariables 
      case Call(target, args, typeArgs) => target.freevars ++ (args flatMap { _.freevars })
      case Parallel(f, g) => f.freevars ++ g.freevars
      case Sequence(f, g) => f.freevars ++ shift(g.freevars, 1)
      case LateBind(f, g) => shift(f.freevars, 1) ++ g.freevars
      case Limit(f) => f.freevars
      case Otherwise(f, g) => f.freevars ++ g.freevars
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
    * Ignores any UnboundVariables.
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
      case UnboundVariable(name) => UnboundVariable(name) // subst ignores UnboundVariables
      case Call(target, args, typeArgs) => {
        Call(target.subst(ctx).asInstanceOf[Argument], args map { _.subst(ctx).asInstanceOf[Argument] }, typeArgs)
      }
      case Parallel(f, g) => Parallel(f.subst(ctx), g.subst(ctx))
      case Sequence(f, g) => Sequence(f.subst(ctx), g.subst(None :: ctx))
      case LateBind(f, g) => LateBind(f.subst(None :: ctx), g.subst(ctx))
      case Limit(f) => Limit(f.subst(ctx))
      case Otherwise(f, g) => Otherwise(f.subst(ctx), g.subst(ctx))
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
sealed case class Stop() extends Expression
sealed case class Call(target: Argument, args: List[Argument], typeArgs: Option[List[Type]]) extends Expression
sealed case class Parallel(left: Expression, right: Expression) extends Expression
sealed case class Sequence(left: Expression, right: Expression) extends Expression with hasOptionalVariableName
sealed case class LateBind(left: Expression, right: Expression) extends Expression with hasOptionalVariableName
sealed case class Limit(f: Expression) extends Expression
sealed case class Otherwise(left: Expression, right: Expression) extends Expression
sealed case class DeclareDefs(unclosedVars: List[Int], defs: List[Def], body: Expression) extends Expression
sealed case class DeclareType(t: Type, body: Expression) extends Expression with hasOptionalVariableName
sealed case class HasType(body: Expression, expectedType: Type) extends Expression
sealed case class Hole(context: Map[String, Argument], typecontext: Map[String, Type]) extends Expression
sealed case class VtimeZone(timeOrder: Argument, body: Expression) extends Expression

sealed abstract class Argument extends Expression
sealed case class Constant(value: AnyRef) extends Argument
sealed case class Variable(index: Int) extends Argument with hasOptionalVariableName {
  require(index >= 0)
}
sealed case class UnboundVariable(name: String) extends Argument with hasOptionalVariableName {
  optionalVariableName = Some(name)
}

sealed abstract class Type extends NamelessAST with NamelessToNamed {
  lazy val withNames: named.Type = namelessToNamed(this, Nil)
}
sealed case class Top() extends Type
sealed case class Bot() extends Type
sealed case class TypeVar(index: Int) extends Type with hasOptionalVariableName
sealed case class TupleType(elements: List[Type]) extends Type
sealed case class RecordType(entries: Map[String, Type]) extends Type
sealed case class TypeApplication(tycon: Int, typeactuals: List[Type]) extends Type
sealed case class AssertedType(assertedType: Type) extends Type
sealed case class FunctionType(typeFormalArity: Int, argTypes: List[Type], returnType: Type) extends Type
sealed case class TypeAbstraction(typeFormalArity: Int, t: Type) extends Type
sealed case class ImportedType(classname: String) extends Type
sealed case class ClassType(classname: String) extends Type
sealed case class VariantType(typeFormalArity: Int, variants: List[(String, List[Type])]) extends Type
sealed case class UnboundTypeVariable(name: String) extends Type with hasOptionalVariableName {
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

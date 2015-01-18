//
// Nameless.scala -- Nameless representation of OIL syntax
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.nameless

import orc.ast.oil._
import orc.ast.AST
import orc.ast.hasOptionalVariableName
import orc.values.Field

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
    case Graft(value, body) => List(value, body)
    case Trim(f) => List(f)
    case left ow right => List(left, right)
    case New(f) => List(f)
    case FieldAccess(o, f) => List(o)
    case DeclareCallables(_, defs, body) => defs ::: List(body)
    case VtimeZone(timeOrder, body) => List(timeOrder, body)
    case HasType(body, expectedType) => List(body, expectedType)
    case DeclareType(t, body) => List(t, body)
    case Callable(_, _, body, argtypes, returntype) => {
      body :: (argtypes.toList.flatten ::: returntype.toList)
    }
    case Class(bindings) => bindings.values
    case DeclareClasses(clss, body) => clss :+ body
    case TupleType(elements) => elements
    case FunctionType(_, argTypes, returnType) => argTypes :+ returnType
    case TypeApplication(_, typeactuals) => typeactuals
    case AssertedType(assertedType) => List(assertedType)
    case TypeAbstraction(_, t) => List(t)
    case RecordType(entries) => entries.values
    case VariantType(_, variants) => {
      for ((_, variant) <- variants; t <- variant) yield t
    }
    case Constant(_) | UnboundVariable(_) | Variable(_) | Classvar(_) | Hole(_, _) | Stop() => Nil
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
      case Graft(g, f) => shift(f.freevars, 1) ++ g.freevars
      case Trim(f) => f.freevars
      case f ow g => f.freevars ++ g.freevars
      case New(s) => s.freevars
      case DeclareClasses(clss, body) => clss.flatMap(_.freevars).toSet ++ shift(body.freevars, clss.length)
      case DeclareCallables(openvars, defs, body) => openvars.toSet ++ shift(body.freevars, defs.length)
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
      case Graft(g, f) => Graft(g.subst(ctx), f.subst(None :: ctx))
      case Trim(f) => Trim(f.subst(ctx))
      case f ow g => f.subst(ctx) ow g.subst(ctx)
      case DeclareCallables(openvars, defs, body) => {
        val newctx = (for (_ <- defs) yield None).toList ::: ctx
        val newdefs = {
          val defctx = (for (_ <- openvars) yield None).toList ::: newctx
          defs map { _.subst(defctx) }
        }
        val newbody = body.subst(newctx)
        DeclareCallables(openvars, newdefs, newbody)
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
// Note: recommend reading Graft(f, g) as "graft f into g".
case class Graft(value: Expression, body: Expression) extends Expression with hasOptionalVariableName
case class Trim(f: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression

case class New(structure: ObjectStructure) extends Expression

case class DeclareClasses(defs: List[Class], body: Expression) extends Expression

// Callable should contain all Sites or all Defs and not a mix.
case class DeclareCallables(unclosedVars: List[Int], defs: List[Callable], body: Expression) extends Expression
case class DeclareType(t: Type, body: Expression) extends Expression with hasOptionalVariableName

case class HasType(body: Expression, expectedType: Type) extends Expression
case class Hole(context: Map[String, Argument], typecontext: Map[String, Type]) extends Expression
case class VtimeZone(timeOrder: Argument, body: Expression) extends Expression
case class FieldAccess(obj: Argument, field: Field) extends Expression

sealed abstract class Argument extends Expression
case class Constant(value: AnyRef) extends Argument
case class Variable(index: Int) extends Argument with hasOptionalVariableName {
  require(index >= 0)
  override def toString = optionalVariableName.getOrElse("") + "#" + index
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

sealed trait ObjectStructure
  extends NamelessAST
  with hasFreeVars

case class Classvar(index: Int) extends ObjectStructure with hasOptionalVariableName {
  require(index >= 0)
  lazy val freevars: Set[Int] = Set(index)
}

case class Class(
  val bindings: Map[Field, Expression])
  extends NamelessAST
  with hasFreeVars
  with hasOptionalVariableName {
  lazy val freevars: Set[Int] = {
    shift(bindings.values.flatMap(_.freevars).toSet, 1)
  }
}

sealed abstract class Callable extends NamelessAST
  with hasFreeVars
  with hasOptionalVariableName {
  /* Get the free vars of the body, then bind the arguments */
  lazy val freevars: Set[Int] = shift(body.freevars, arity)

  val typeFormalArity: Int
  val arity: Int
  val body: Expression
  val argTypes: Option[List[Type]]
  val returnType: Option[Type]

  def copy(typeFormalArity: Int, arity: Int, body: Expression, argTypes: Option[List[Type]], returnType: Option[Type]): Callable

  def subst(ctx: List[Option[AnyRef]]): Callable = {
    copy(typeFormalArity, arity, body.subst(substCtx(ctx)), argTypes, returnType)
  }

  protected def substCtx(ctx: List[Option[AnyRef]]) = (for (_ <- List.range(0, arity)) yield None).toList ::: ctx
}
object Callable {
  def unapply(value: Callable) = {
    Some((value.typeFormalArity, value.arity, value.body, value.argTypes, value.returnType))
  }
}

case class Def(typeFormalArity: Int, arity: Int, body: Expression, argTypes: Option[List[Type]], returnType: Option[Type]) extends Callable {
  def copy(typeFormalArity: Int = typeFormalArity, arity: Int = arity, body: Expression = body, argTypes: Option[List[Type]] = argTypes, returnType: Option[Type] = returnType): Def = {
    Def(typeFormalArity, arity, body, argTypes, returnType)
  }
}
case class Site(typeFormalArity: Int, arity: Int, body: Expression, argTypes: Option[List[Type]], returnType: Option[Type]) extends Callable {
  def copy(typeFormalArity: Int = typeFormalArity, arity: Int = arity, body: Expression = body, argTypes: Option[List[Type]] = argTypes, returnType: Option[Type] = returnType): Site = {
    Site(typeFormalArity, arity, body, argTypes, returnType)
  }
}

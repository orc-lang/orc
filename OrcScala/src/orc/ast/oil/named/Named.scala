//
// Named.scala -- Named representation of OIL syntax
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

package orc.ast.oil.named

import scala.language.reflectiveCalls

import orc.ast.{ AST, hasOptionalVariableName }
import orc.ast.oil.nameless

sealed abstract class NamedAST extends AST with NamedToNameless {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()

  override val subtrees: Iterable[NamedAST] = this match {
    case Call(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case Parallel(left, right) => List(left, right)
    case Sequence(left, x, right) => List(left, x, right)
    case LateBind(left, x, right) => List(left, x, right)
    case Limit(f) => List(f)
    case Otherwise(left, right) => List(left, right)
    case DeclareDefs(defs, body) => defs ::: List(body)
    case VtimeZone(timeOrder, body) => List(timeOrder, body)
    case HasType(body, expectedType) => List(body, expectedType)
    case DeclareType(u, t, body) => List(u, t, body)
    case Def(f, formals, body, typeformals, argtypes, returntype) => {
      f :: (formals ::: (List(body) ::: typeformals ::: argtypes.toList.flatten ::: returntype.toList))
    }
    case TupleType(elements) => elements
    case FunctionType(_, argTypes, returnType) => argTypes :+ returnType
    case TypeApplication(tycon, typeactuals) => tycon :: typeactuals
    case AssertedType(assertedType) => List(assertedType)
    case TypeAbstraction(typeformals, t) => typeformals ::: List(t)
    case RecordType(entries) => entries.values
    case VariantType(self, typeformals, variants) => {
      self :: typeformals ::: (for ((_, variant) <- variants; t <- variant) yield t)
    }
    case Constant(_) | UnboundVar(_) | Hole(_, _) | Stop() => Nil
    case Bot() | ClassType(_) | ImportedType(_) | Top() | UnboundTypevar(_) => Nil
    case _: BoundVar | _: BoundTypevar => Nil
    case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in NamedAST.subtrees")
  }

}

sealed abstract class Expression
  extends NamedAST
  with NamedInfixCombinators
  with hasVars
  with Substitution[Expression]
  with ContextualSubstitution
  with Guarding {
  lazy val withoutNames: nameless.Expression = namedToNameless(this, Nil, Nil)
}

sealed case class Stop() extends Expression
sealed case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
sealed case class Parallel(left: Expression, right: Expression) extends Expression
sealed case class Sequence(left: Expression, x: BoundVar, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
sealed case class LateBind(left: Expression, x: BoundVar, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
sealed case class Limit(expr: Expression) extends Expression
sealed case class Otherwise(left: Expression, right: Expression) extends Expression
sealed case class DeclareDefs(defs: List[Def], body: Expression) extends Expression
sealed case class DeclareType(name: BoundTypevar, t: Type, body: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(name, this) }
sealed case class HasType(body: Expression, expectedType: Type) extends Expression
sealed case class Hole(context: Map[String, Argument], typecontext: Map[String, Type]) extends Expression {
  def apply(e: Expression): Expression = e.subst(context, typecontext)
}
sealed case class VtimeZone(timeOrder: Argument, body: Expression) extends Expression

/* Match an expression with exactly one hole.
 * Matches as Module(f), where f is a function which takes
 * a hole-filling expression and returns this expression
 * with the hole filled.
 */
object Module {
  def unapply(e: Expression): Option[Expression => Expression] = {
    if (countHoles(e) == 1) {
      def fillWith(fill: Expression): Expression = {
        val transform = new NamedASTTransform {
          override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
            case h: Hole => h(fill)
          }
        }
        transform(e)
      }
      Some(fillWith)
    } else {
      None
    }
  }

  def countHoles(e: Expression): Int = {
    var holes = 0
    val search = new NamedASTTransform {
      override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
        case h: Hole => holes += 1; h
      }
    }
    search(e)
    holes
  }
}

sealed abstract class Argument extends Expression
sealed case class Constant(value: AnyRef) extends Argument
sealed trait Var extends Argument with hasOptionalVariableName
sealed case class UnboundVar(name: String) extends Var {
  optionalVariableName = Some(name)
}
class BoundVar(optionalName: Option[String] = None) extends Var with hasOptionalVariableName {

  optionalVariableName = optionalName

  def productIterator = optionalVariableName.toList.iterator
}

sealed case class Def(name: BoundVar, formals: List[BoundVar], body: Expression, typeformals: List[BoundTypevar], argtypes: Option[List[Type]], returntype: Option[Type])
  extends NamedAST
  with hasFreeVars
  with hasFreeTypeVars
  with hasOptionalVariableName
  with Substitution[Def] {
  transferOptionalVariableName(name, this)
  lazy val withoutNames: nameless.Def = namedToNameless(this, Nil, Nil)

  def copy(name: BoundVar = name,
    formals: List[BoundVar] = formals,
    body: Expression = body,
    typeformals: List[BoundTypevar] = typeformals,
    argtypes: Option[List[Type]] = argtypes,
    returntype: Option[Type] = returntype) = {
    this ->> Def(name, formals, body, typeformals, argtypes, returntype)
  }
}

sealed abstract class Type
  extends NamedAST
  with hasFreeTypeVars
  with Substitution[Type] {
  lazy val withoutNames: nameless.Type = namedToNameless(this, Nil)
}
sealed case class Top() extends Type
sealed case class Bot() extends Type
sealed case class TupleType(elements: List[Type]) extends Type
sealed case class RecordType(entries: Map[String, Type]) extends Type
sealed case class TypeApplication(tycon: Type, typeactuals: List[Type]) extends Type
sealed case class AssertedType(assertedType: Type) extends Type
sealed case class FunctionType(typeformals: List[BoundTypevar], argtypes: List[Type], returntype: Type) extends Type
sealed case class TypeAbstraction(typeformals: List[BoundTypevar], t: Type) extends Type
sealed case class ImportedType(classname: String) extends Type
sealed case class ClassType(classname: String) extends Type
sealed case class VariantType(self: BoundTypevar, typeformals: List[BoundTypevar], variants: List[(String, List[Type])]) extends Type

sealed trait Typevar extends Type with hasOptionalVariableName
sealed case class UnboundTypevar(name: String) extends Typevar {
  optionalVariableName = Some(name)
}
class BoundTypevar(optionalName: Option[String] = None) extends Typevar with hasOptionalVariableName {

  optionalVariableName = optionalName

  def productIterator = optionalVariableName.toList.iterator
}

object Conversions {
  /** Given an expression of the form:
    *
    * E <x1<| e1
    * ...
    * <xn<| en
    *
    * where E is not a latebind,
    * return E and (x1,e1), ... , (xn,en)
    *
    * If E is not of this form,
    * return E and Nil.
    */
  def partitionLatebind(expr: Expression): (List[(Argument, Expression)], Expression) = {
    expr match {
      case LateBind(left, x, right) => {
        val (bindings, core) = partitionLatebind(left)
        ((x, right) :: bindings, core)
      }
      case _ => (Nil, expr)
    }
  }

}


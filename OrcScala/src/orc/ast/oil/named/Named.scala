//
// Named.scala -- Named representation of OIL syntax
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

package orc.ast.oil.named

import scala.language.reflectiveCalls
import orc.ast.oil._
import orc.ast.AST
import orc.ast.hasOptionalVariableName
import orc.ast.hasAutomaticVariableName
import orc.values

sealed abstract class NamedAST extends AST with NamedToNameless {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()

  override val subtrees: Iterable[NamedAST] = this match {
    case Call(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case left || right => List(left, right)
    case Sequence(left, x, right) => List(left, x, right)
    case LateBind(left, x, right) => List(left, x, right)
    case Limit(f) => List(f)
    case left ow right => List(left, right)
    case DeclareDefs(defs, body) => defs ::: List(body)
    case VtimeZone(timeOrder, body) => List(timeOrder, body)
    case FieldAccess(o, f) => List(o)
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

case class Stop() extends Expression
case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, x: BoundVar, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
case class LateBind(left: Expression, x: BoundVar, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
case class Limit(expr: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression
case class DeclareDefs(defs: List[Def], body: Expression) extends Expression
case class DeclareType(name: BoundTypevar, t: Type, body: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(name, this) }
case class HasType(body: Expression, expectedType: Type) extends Expression
case class Hole(context: Map[String, Argument], typecontext: Map[String, Type]) extends Expression {
  def apply(e: Expression): Expression = e.subst(context, typecontext)
}
case class VtimeZone(timeOrder: Argument, body: Expression) extends Expression

/** Read the value from a field.
  */
case class FieldAccess(obj: Argument, field: values.Field) extends Expression

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
case class Constant(value: AnyRef) extends Argument
trait Var extends Argument with hasOptionalVariableName
case class UnboundVar(name: String) extends Var {
  optionalVariableName = Some(name)
}
class BoundVar(optionalName: Option[String] = None) extends Var with hasAutomaticVariableName {

  optionalVariableName = optionalName
  autoName("v")
  assert(optionalVariableName.isDefined)

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
case class Top() extends Type
case class Bot() extends Type
case class TupleType(elements: List[Type]) extends Type
case class RecordType(entries: Map[String, Type]) extends Type
case class TypeApplication(tycon: Type, typeactuals: List[Type]) extends Type
case class AssertedType(assertedType: Type) extends Type
case class FunctionType(typeformals: List[BoundTypevar], argtypes: List[Type], returntype: Type) extends Type
case class TypeAbstraction(typeformals: List[BoundTypevar], t: Type) extends Type
case class ImportedType(classname: String) extends Type
case class ClassType(classname: String) extends Type
case class VariantType(self: BoundTypevar, typeformals: List[BoundTypevar], variants: List[(String, List[Type])]) extends Type

trait Typevar extends Type with hasOptionalVariableName
case class UnboundTypevar(name: String) extends Typevar {
  optionalVariableName = Some(name)
}
class BoundTypevar(optionalName: Option[String] = None) extends Typevar with hasAutomaticVariableName {

  optionalVariableName = optionalName
  autoName("t")

  def productIterator = optionalVariableName.toList.iterator
}

object Conversions {
  /** Given an expression of the form:
    *
    * E <x1<| e1
    * ...
    *  <xn<| en
    *
    * where E is not a latebind,
    * return E and (x1,e1), ... , (xn,en)
    *
    * If E is not of this form,
    * return E and Nil.
    */
  def partitionLatebind(expr: Expression): (List[(Argument, Expression)], Expression) = {
    expr match {
      case left < x <| right => {
        val (bindings, core) = partitionLatebind(left)
        ((x, right) :: bindings, core)
      }
      case _ => (Nil, expr)
    }
  }

}


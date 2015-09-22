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

package orc.ast.oil4c.named

import scala.language.reflectiveCalls

import orc.ast.oil._
import orc.ast.AST
import orc.ast.hasOptionalVariableName
import orc.values

sealed abstract class NamedAST extends AST {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()

  override val subtrees: Iterable[NamedAST] = this match {
    case Call(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case left || right => List(left, right)
    case Sequence(left, x, right) => List(left, x, right)
    case Prune(left, x, right) => List(left, x, right)
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
  with ContextualSubstitution {
}

case class Stop() extends Expression
case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, x: BoundVar, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
case class Prune(left: Expression, x: BoundVar, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
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
class BoundTypevar(optionalName: Option[String] = None) extends Typevar with hasOptionalVariableName {

  optionalVariableName = optionalName

  def productIterator = optionalVariableName.toList.iterator
}

object Conversions {

  /** Given (e1, ... , en) and f, return:
    *
    * f(x1, ... , xn) <x1< e1
    *               ...
    *                <xn< en
    *
    * As an optimization, if any e is already an argument, no << binder is generated for it.
    */
  def unfold(es: List[Expression], makeCore: List[Argument] => Expression): Expression = {

    def expand(es: List[Expression]): (List[Argument], Expression => Expression) =
      es match {
        case (a: Argument) :: rest => {
          val (args, bindRest) = expand(rest)
          (a :: args, bindRest)
        }
        case g :: rest => {
          val (args, bindRest) = expand(rest)
          val x = new BoundVar()
          (x :: args, bindRest(_) < x < g)
        }
        case Nil => (Nil, e => e)
      }

    val (args, bind) = expand(es)
    bind(makeCore(args))
  }

  /** Given an expression of the form:
    *
    * E <x1< e1
    * ...
    *  <xn< en
    *
    * where E is not a prune,
    * return E and (x1,e1), ... , (xn,en)
    *
    * If E is not of this form,
    * return E and Nil.
    */
  def partitionPrune(expr: Expression): (List[(Argument, Expression)], Expression) = {
    expr match {
      case left < x < right => {
        val (bindings, core) = partitionPrune(left)
        ((x, right) :: bindings, core)
      }
      case _ => (Nil, expr)
    }
  }

}

/* Special syntactic forms, which conceptually 'reverse' some of
 * the translations performed earlier in compilation, because
 * it is sometimes easier to work with the unencoded versions.
 *
 * Each form is an object with an unapply (decode to special
 * form) and an apply (encode to canonical form) method. Thus,
 * they can be treated like case classes, except that construction
 * instantiates an entire subtree rather than a single class,
 * and similarly, deconstruction matches an entire subtree.
 */

/* A call with argument unfolding reversed.
 *
 * Matching this pattern can take O(N^2) steps,
 * where N is the depth of a series of left-associative
 * nested pruning combinators. That is, it is of the form
 * f <x1< g1 <x2< g2 ... <xN< gN
 *
 * The use cases for this pattern could be rewritten
 * to more complex and less maintainable O(N) solutions,
 * but I figured it wasn't worth it. -dkitchin
 */
object FoldedCall {

  def unapply(expr: Expression): Option[(Expression, List[Expression], Option[List[Type]])] = {
    Conversions.partitionPrune(expr) match {
      case (Nil, Call(target, args, typeArgs)) => Some((target, args, typeArgs))
      case (bindings, Call(target, args, typeArgs)) => {
        val exprMap = bindings.toMap
        if ((exprMap.keySet) subsetOf (args.toSet)) {
          val targetExpression = exprMap.getOrElse(target, target)
          val argExpressions = args map { arg => exprMap.getOrElse(arg, arg) }
          Some((targetExpression, argExpressions, typeArgs))
        } else {
          None
        }
      }
      case _ => None
    }
  }

  def apply(targetExpression: Expression, argExpressions: List[Expression], typeArgs: Option[List[Type]]): Expression = {
    Conversions.unfold(targetExpression :: argExpressions, { x => Call(x.head, x.tail, typeArgs) })
  }

}

/* An anonymous function with lambda translation reversed. */
object FoldedLambda {

  def unapply(expr: Expression): Option[(List[BoundVar], Expression, List[BoundTypevar], Option[List[Type]], Option[Type])] = {
    expr match {
      case DeclareDefs(List(Def(m, formals, body, typeFormals, argTypes, returnType)), n: BoundVar) if (m eq n) => {
        Some((formals, body, typeFormals, argTypes, returnType))
      }
      case _ => None
    }
  }

  /* FoldedLambda can only be constructed with full type annotations */
  def apply(formals: List[BoundVar], body: Expression, typeFormals: List[BoundTypevar], argTypes: Option[List[Type]], returnType: Option[Type]): Expression = {
    val dummyName = new BoundVar()
    val dummyDef = Def(dummyName, formals, body, typeFormals, argTypes, returnType)
    DeclareDefs(List(dummyDef), dummyName)
  }

}

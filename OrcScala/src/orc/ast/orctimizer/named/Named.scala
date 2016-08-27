//
// Named.scala -- Named representation of Orctimizer syntax
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

package orc.ast.orctimizer.named

import scala.language.reflectiveCalls
import orc.ast.orctimizer._
import orc.ast.AST
import orc.ast.hasOptionalVariableName
import orc.ast.hasAutomaticVariableName
import orc.values

// TODO: Consider porting ODO FieldAccess combinator to this.
// TODO: Consider porting Porc Tuple access sites. Or should it be a varient of FieldAccess (_1, _2, ...)?
// This issue with this is that while it's easy to detect field access (Field constants don't appear anywhere else)
// it's hard to tell what is a tuple access.

sealed abstract class NamedAST extends AST with WithContextInfixCombinator {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()

  override val subtrees: Iterable[NamedAST] = this match {
    case CallDef(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case CallSite(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case left || right => List(left, right)
    case left > x > right => List(left, x, right)
    case Trim(f) => List(f)
    case Force(xs, vs, _, e) => xs ::: vs ::: List(e)
    case IfDef(v, l, r) => List(v, l, r)
    case Future(x, f, g) => List(f, g)
    case left Otherwise right => List(left, right)
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
    case Constant(_) | UnboundVar(_) | Stop() => Nil
    case Bot() | ClassType(_) | ImportedType(_) | Top() | UnboundTypevar(_) => Nil
    case _: BoundVar | _: BoundTypevar => Nil
    case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in NamedAST.subtrees")
  }

}

sealed abstract class Expression
  extends NamedAST
  with NamedInfixCombinators
  //with hasVars
  with Substitution[Expression]
  //with ContextualSubstitution
  //with Guarding 
  {
  //lazy val withoutNames: nameless.Expression = namedToNameless(this, Nil, Nil)

  /* Note: As is evident from the type, UnboundVars are not included in this set */
  lazy val freeVars: Set[BoundVar] = {
    val varset = new scala.collection.mutable.HashSet[BoundVar]()
    val collect = new NamedASTTransform {
      override def onArgument(context: List[BoundVar]) = {
        case x: BoundVar => (if (context contains x) {} else { varset += x }); x
      }
    }
    collect(this)
    Set.empty ++ varset
  }
}

case class Stop() extends Expression
case class Future(x: BoundVar, left: Expression, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
case class Force(xs: List[BoundVar], vs: List[Argument], publishForce: Boolean, expr: Expression) extends Expression
object Force {
  def apply(x: BoundVar, v:Argument, publishForce: Boolean, expr: Expression): Force = 
    Force(List(x), List(v), publishForce, expr)
  def asExpr(v: Argument, publishForce: Boolean = true) = {
    val x = new BoundVar()
    Force(List(x), List(v), publishForce, x)
  }
}

case class IfDef(v: Argument, left: Expression, right: Expression) extends Expression

case class CallDef(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
case class CallSite(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression

case class Parallel(left: Expression, right: Expression) extends Expression
case class Branch(left: Expression, x: BoundVar, right: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
case class Trim(expr: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression

case class DeclareDefs(defs: List[Def], body: Expression) extends Expression
case class DeclareType(name: BoundTypevar, t: Type, body: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(name, this) }
case class HasType(body: Expression, expectedType: Type) extends Expression
case class VtimeZone(timeOrder: Argument, body: Expression) extends Expression

/** Read the value from a field.
  */
case class FieldAccess(obj: Argument, field: values.Field) extends Expression


sealed abstract class Argument extends Expression
case class Constant(value: AnyRef) extends Argument
sealed trait Var extends Argument with hasOptionalVariableName
case class UnboundVar(name: String) extends Var {
  optionalVariableName = Some(name)
}
class BoundVar(optionalName: Option[String] = None) extends Var with hasAutomaticVariableName {

  optionalVariableName = optionalName
  autoName("ov")

  def productIterator = optionalVariableName.toList.iterator
}

sealed case class Def(name: BoundVar, formals: List[BoundVar], body: Expression, typeformals: List[BoundTypevar], argtypes: Option[List[Type]], returntype: Option[Type])
  extends NamedAST
  //with hasFreeVars
  //with hasFreeTypeVars
  with hasOptionalVariableName
  with Substitution[Def] 
  {
  //TODO: Does Def need to have the closed variables listed here? Probably not unless we are type checking.
  
  transferOptionalVariableName(name, this)
  //lazy val withoutNames: nameless.Def = namedToNameless(this, Nil, Nil)

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
  //with hasFreeTypeVars
  with Substitution[Type] 
  {
  //lazy val withoutNames: nameless.Type = namedToNameless(this, Nil)
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

sealed trait Typevar extends Type with hasOptionalVariableName
case class UnboundTypevar(name: String) extends Typevar {
  optionalVariableName = Some(name)
}
class BoundTypevar(optionalName: Option[String] = None) extends Typevar with hasAutomaticVariableName {

  optionalVariableName = optionalName
  autoName("t")

  def productIterator = optionalVariableName.toList.iterator
}

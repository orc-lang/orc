//
// Named.scala -- Named representation of Orctimizer syntax
// Project OrcScala
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.orctimizer.named

import scala.language.reflectiveCalls
import orc.ast.orctimizer._
import orc.ast.ASTForSwivel
import orc.ast.hasOptionalVariableName
import orc.ast.hasAutomaticVariableName
import orc.values
import orc.ast.PrecomputeHashcode
import swivel.{ root, branch, leaf }
import swivel.subtree
import swivel.replacement
import swivel.EmptyFunction
import scala.PartialFunction

trait Transform extends swivel.TransformFunction {
  val onExpression: PartialFunction[Expression.Z, Expression] = {
    case a: Argument.Z if onArgument.isDefinedAt(a) => onArgument(a)
  }
  def apply(e: Expression.Z) = transformWith[Expression.Z, Expression](e)(this, onExpression)

  val onArgument: PartialFunction[Argument.Z, Argument] = PartialFunction.empty
  def apply(e: Argument.Z) = transformWith[Argument.Z, Argument](e)(this, onArgument)

  val onCallable: PartialFunction[Callable.Z, Callable] = PartialFunction.empty
  def apply(e: Callable.Z) = transformWith[Callable.Z, Callable](e)(this, onCallable)

  val onFieldValue: PartialFunction[FieldValue.Z, FieldValue] = PartialFunction.empty
  def apply(e: FieldValue.Z) = transformWith[FieldValue.Z, FieldValue](e)(this, onFieldValue)

  val onType: PartialFunction[Type.Z, Type] = PartialFunction.empty
  def apply(e: Type.Z) = transformWith[Type.Z, Type](e)(this, onType)

  def apply(e: NamedAST.Z): NamedAST = e match {
    case e: Expression.Z => apply(e)
    case e: Callable.Z => apply(e)
    case e: FieldValue.Z => apply(e)
    case e: Type.Z => apply(e)
  }
}

// TODO: Consider porting Porc Tuple access sites. Or should it be a variant of FieldAccess (_1, _2, ...)?
// This issue with this is that while it's easy to detect field access (Field constants don't appear anywhere else)
// it's hard to tell what is a tuple access.
@root @transform[Transform]
sealed abstract class NamedAST extends ASTForSwivel {
  def prettyprint() = (new PrettyPrint()).reduce(this).toString()
  override def toString() = prettyprint()

  def boundVars: Set[BoundVar] = Set()
}

object NamedAST {
  import scala.language.implicitConversions
  implicit def zipper2Value(z: NamedAST.Z): z.Value = z.value
}

@branch @replacement[Expression]
sealed abstract class Expression
  extends NamedAST
  with NamedInfixCombinators
  //with hasVars
  with Substitution[Expression]
  //with ContextualSubstitution
  //with Guarding
  with PrecomputeHashcode {
  this: Product =>

  /** Get the set of free variables in an expression.
    *
    * Note: As is evident from the type, UnboundVars are not included in this set
    */
  lazy val freeVars: Set[BoundVar] = {
    val varset = new scala.collection.mutable.HashSet[BoundVar]()
    val collect = new Transform {
      override val onArgument = {
        case x: BoundVar.Z => (if (x.context contains x.value) {} else { varset += x.value }); x.value
      }
    }
    collect(this.toZipper())
    Set.empty ++ varset
  }
}

object Expression {
  class Z {
    def context = {
      parents.flatMap(_.value.boundVars)
    }
    def freeVars = {
      value.freeVars
    }
  }
}

@leaf @transform
final case class Stop() extends Expression
@leaf @transform
final case class Future(@subtree expr: Expression) extends Expression

@leaf @transform
final case class Force(xs: Seq[BoundVar], @subtree vs: Seq[Argument], publishForce: Boolean, @subtree expr: Expression) extends Expression {
  def varForArg(v: Argument) = {
    try {
      xs(vs.indexOf(v))
    } catch {
      case _: IndexOutOfBoundsException =>
        throw new IllegalArgumentException(s"Unknown argument: $v")
    }
  }
  def argForVar(x: BoundVar) = {
    try {
      vs(xs.indexOf(x))
    } catch {
      case _: IndexOutOfBoundsException =>
        throw new IllegalArgumentException(s"Unknown variable: $x")
    }
  }

  def toMap = (xs zip vs).toMap

  override def boundVars: Set[BoundVar] = xs.toSet
}
object Force {
  def apply(x: BoundVar, v: Argument, publishForce: Boolean, expr: Expression): Force =
    Force(List(x), List(v), publishForce, expr)
  def asExpr(v: Argument, publishForce: Boolean = true) = {
    val x = new BoundVar()
    Force(List(x), List(v), publishForce, x)
  }
}

@leaf @transform
final case class IfDef(@subtree v: Argument, @subtree left: Expression, @subtree right: Expression) extends Expression

@branch
sealed abstract class Call extends Expression {
  this: Product =>
  val target: Argument
  val args: Seq[Argument]
  val typeargs: Option[Seq[Type]]
}
object Call {
  def unapply(c: Call) = 
    if (c != null)
      Some((c.target, c.args, c.typeargs))
    else
      None
  
  class Z {
  }
  object Z {
    def unapply(value: Call.Z) = {
      value match {
        case CallDef.Z(target, args, typeargs) =>
          Some((target, args, typeargs))
        case CallSite.Z(target, args, typeargs) =>
          Some((target, args, typeargs))
        case _ =>
          None
      }
    }
  }
}
@leaf @transform
final case class CallDef(@subtree target: Argument, @subtree args: Seq[Argument], @subtree typeargs: Option[Seq[Type]]) extends Call
@leaf @transform
final case class CallSite(@subtree target: Argument, @subtree args: Seq[Argument], @subtree typeargs: Option[Seq[Type]]) extends Call

@leaf @transform
final case class Parallel(@subtree left: Expression, @subtree right: Expression) extends Expression
@leaf @transform
final case class Branch(@subtree left: Expression, x: BoundVar, @subtree right: Expression) extends Expression
  with hasOptionalVariableName {
  transferOptionalVariableName(x, this)

  override def boundVars: Set[BoundVar] = Set(x)
}
@leaf @transform
final case class Trim(@subtree expr: Expression) extends Expression
@leaf @transform
final case class Otherwise(@subtree left: Expression, @subtree right: Expression) extends Expression

@leaf @transform
final case class DeclareCallables(@subtree defs: Seq[Callable], @subtree body: Expression) extends Expression {
  override def boundVars: Set[BoundVar] = defs.map(_.name).toSet
}
@leaf @transform
final case class DeclareType(name: BoundTypevar, @subtree t: Type, @subtree body: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(name, this) }
@leaf @transform
final case class HasType(@subtree body: Expression, @subtree expectedType: Type) extends Expression

@leaf @transform
final case class New(self: BoundVar, @subtree selfType: Option[Type], @subtree bindings: Map[values.Field, FieldValue], @subtree objType: Option[Type]) extends Expression {
  override def boundVars: Set[BoundVar] = Set(self)
}

@branch @replacement[FieldValue]
sealed abstract class FieldValue extends NamedAST with PrecomputeHashcode {
  this: Product =>
}

@leaf @transform
final case class FieldFuture(@subtree expr: Expression) extends FieldValue
@leaf @transform
final case class FieldArgument(@subtree expr: Argument) extends FieldValue

/** Read the value from a field.
  */
@leaf @transform
final case class FieldAccess(@subtree obj: Argument, field: values.Field) extends Expression

@branch
sealed abstract class Argument extends Expression {
  this: Product =>
}
@leaf @transform
final case class Constant(constantValue: AnyRef) extends Argument
@branch
sealed abstract class Var extends Argument with hasOptionalVariableName {
  this: Product =>
}
@leaf @transform
final case class UnboundVar(name: String) extends Var {
  optionalVariableName = Some(name)
}
@leaf @transform
final class BoundVar(val optionalName: Option[String] = None) extends Var with hasAutomaticVariableName with Product {
  // Members declared in scala.Equals
  def canEqual(that: Any): Boolean = that.isInstanceOf[BoundVar]

  // Members declared in scala.Product
  def productArity: Int = 1
  def productElement(n: Int): Any = optionalName

  optionalVariableName = optionalName
  autoName("ov")
}

@branch @replacement[Callable]
sealed abstract class Callable
  extends NamedAST
  with hasOptionalVariableName
  with Substitution[Callable]
  with PrecomputeHashcode {
  this: Product =>

  val name: BoundVar
  val formals: Seq[BoundVar]
  val body: Expression
  val typeformals: Seq[BoundTypevar]
  val argtypes: Option[Seq[Type]]
  val returntype: Option[Type]

  def copy(name: BoundVar = name,
    formals: Seq[BoundVar] = formals,
    body: Expression = body,
    typeformals: Seq[BoundTypevar] = typeformals,
    argtypes: Option[Seq[Type]] = argtypes,
    returntype: Option[Type] = returntype): Callable

  override def boundVars: Set[BoundVar] = formals.toSet
}
object Callable {
  def unapply(value: Callable) = {
    if (value != null) {
      Some((value.name, value.formals, value.body, value.typeformals, value.argtypes, value.returntype))
    } else {
      None
    }
  }
  
  class Z {
    def name = this match {
      case Def.Z(name, formals, body, typeformals, argtypes, returntype) =>
        name
      case Site.Z(name, formals, body, typeformals, argtypes, returntype) =>
        name
    }
    def formals = this match {
      case Def.Z(name, formals, body, typeformals, argtypes, returntype) =>
        formals
      case Site.Z(name, formals, body, typeformals, argtypes, returntype) =>
        formals
    }
    def body = this match {
      case Def.Z(name, formals, body, typeformals, argtypes, returntype) =>
        body
      case Site.Z(name, formals, body, typeformals, argtypes, returntype) =>
        body
    }
  }
  object Z {
    def unapply(value: Callable.Z) = {
      value match {
        case Def.Z(name, formals, body, typeformals, argtypes, returntype) =>
          Some((name, formals, body, typeformals, argtypes, returntype))
        case Site.Z(name, formals, body, typeformals, argtypes, returntype) =>
          Some((name, formals, body, typeformals, argtypes, returntype))
        case _ =>
          None
      }
    }
  }
}

@leaf @transform
final case class Def(override val name: BoundVar, override val formals: Seq[BoundVar], @subtree override val body: Expression, typeformals: Seq[BoundTypevar], @subtree argtypes: Option[Seq[Type]], @subtree returntype: Option[Type])
  extends Callable {
  //TODO: Does Def need to have the closed variables listed here? Probably not unless we are type checking.

  transferOptionalVariableName(name, this)

  def copy(name: BoundVar = name,
    formals: Seq[BoundVar] = formals,
    body: Expression = body,
    typeformals: Seq[BoundTypevar] = typeformals,
    argtypes: Option[Seq[Type]] = argtypes,
    returntype: Option[Type] = returntype) = {
    this ->> Def(name, formals, body, typeformals, argtypes, returntype)
  }
}

@leaf @transform
final case class Site(override val name: BoundVar, override val formals: Seq[BoundVar], @subtree override val body: Expression, typeformals: Seq[BoundTypevar], @subtree argtypes: Option[Seq[Type]], @subtree returntype: Option[Type])
  extends Callable {
  transferOptionalVariableName(name, this)

  def copy(name: BoundVar = name,
    formals: Seq[BoundVar] = formals,
    body: Expression = body,
    typeformals: Seq[BoundTypevar] = typeformals,
    argtypes: Option[Seq[Type]] = argtypes,
    returntype: Option[Type] = returntype) = {
    this ->> Site(name, formals, body, typeformals, argtypes, returntype)
  }
}

@branch @replacement[Type]
sealed abstract class Type
  extends NamedAST
  //with hasFreeTypeVars
  with Substitution[Type]
  with PrecomputeHashcode {
  this: Product =>
  //lazy val withoutNames: nameless.Type = namedToNameless(this, Nil)
}
@leaf @transform
final case class Top() extends Type
@leaf @transform
final case class Bot() extends Type
@leaf @transform
final case class TupleType(@subtree elements: Seq[Type]) extends Type
@leaf @transform
final case class RecordType(@subtree entries: Map[String, Type]) extends Type
@leaf @transform
final case class TypeApplication(@subtree tycon: Type, @subtree typeactuals: Seq[Type]) extends Type
@leaf @transform
final case class AssertedType(@subtree assertedType: Type) extends Type
@leaf @transform
final case class FunctionType(typeformals: Seq[BoundTypevar], @subtree argtypes: Seq[Type], @subtree returntype: Type) extends Type
@leaf @transform
final case class TypeAbstraction(typeformals: Seq[BoundTypevar], @subtree t: Type) extends Type
@leaf @transform
final case class ImportedType(classname: String) extends Type
@leaf @transform
final case class ClassType(classname: String) extends Type

// FIXME: VariantType will not currently support zipper operations on the variants subtrees.
@leaf @transform
final case class VariantType(self: BoundTypevar, typeformals: Seq[BoundTypevar], variants: Seq[(String, Seq[Type])]) extends Type

@leaf @transform
final case class IntersectionType(@subtree a: Type, @subtree b: Type) extends Type
object IntersectionType {
  def apply(as: Iterable[Type]): Type = {
    as.reduce(IntersectionType(_, _))
  }
}

@leaf @transform
final case class UnionType(@subtree a: Type, @subtree b: Type) extends Type
object UnionType {
  def apply(as: Iterable[Type]): Type = {
    as.reduce(UnionType(_, _))
  }
}

@leaf @transform
final case class NominalType(@subtree supertype: Type) extends Type

@leaf @transform
final case class StructuralType(@subtree members: Map[values.Field, Type]) extends Type

@branch
sealed abstract class Typevar extends Type with hasOptionalVariableName {
  this: Product =>
}
@leaf @transform
final case class UnboundTypevar(name: String) extends Typevar {
  optionalVariableName = Some(name)
}
@leaf @transform
final class BoundTypevar(val optionalName: Option[String] = None) extends Typevar with hasAutomaticVariableName with Product {
  def canEqual(that: Any): Boolean = that.isInstanceOf[BoundTypevar]

  // Members declared in scala.Product
  def productArity: Int = 1
  def productElement(n: Int): Any = optionalName

  optionalVariableName = optionalName
  autoName("T")
}

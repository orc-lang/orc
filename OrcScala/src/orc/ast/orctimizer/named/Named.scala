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

import orc.ast.ASTForSwivel
import orc.ast.hasOptionalVariableName
import orc.ast.hasAutomaticVariableName
import orc.values
import orc.ast.PrecomputeHashcode
import swivel.{ root, branch, leaf }
import swivel.subtree
import swivel.replacement
import scala.PartialFunction

// TODO: Remove "Named" from classes and package tree. There is no nameless Orctimizer.

/** The base class for safe transforms on Orctimizer trees.
  */
trait Transform extends swivel.TransformFunction {
  val onExpression: PartialFunction[Expression.Z, Expression] = {
    case a: Argument.Z if onArgument.isDefinedAt(a) => onArgument(a)
  }
  def apply(e: Expression.Z) = transformWith[Expression.Z, Expression](e)(this, onExpression)

  val onArgument: PartialFunction[Argument.Z, Argument] = PartialFunction.empty
  def apply(e: Argument.Z) = transformWith[Argument.Z, Argument](e)(this, onArgument)

  val onMethod: PartialFunction[Method.Z, Method] = PartialFunction.empty
  def apply(e: Method.Z) = transformWith[Method.Z, Method](e)(this, onMethod)

  val onFieldValue: PartialFunction[FieldValue.Z, FieldValue] = PartialFunction.empty
  def apply(e: FieldValue.Z) = transformWith[FieldValue.Z, FieldValue](e)(this, onFieldValue)

  val onType: PartialFunction[Type.Z, Type] = PartialFunction.empty
  def apply(e: Type.Z) = transformWith[Type.Z, Type](e)(this, onType)

  def apply(e: NamedAST.Z): NamedAST = e match {
    case e: Expression.Z => apply(e)
    case e: Method.Z => apply(e)
    case e: FieldValue.Z => apply(e)
    case e: Type.Z => apply(e)
  }
}

/** The base of all Orctimizer AST including non-expressions.
  */
@root @transform[Transform]
sealed abstract class NamedAST extends ASTForSwivel {
  def prettyprint() = (new PrettyPrint()).reduce(this).toString()
  override def toString() = prettyprint()

  /** @return a set of variables that are bound by this AST node.
    * 				These variables in in scope for any subexpressions.
    */
  def boundVars: Set[BoundVar] = Set()
}

object NamedAST {
  /** The Zipper base for NamedAST.
    *
    * This class contains explicitly added members. Swivel also adds many implicit methods.
    */
  class Z {
    /** @return a set of variables that are bound by this AST node.
      * 				These variables in in scope for any subexpressions.
      *
      * This simply forwards to value.boundVars.
      */
    def boundVars: Set[BoundVar] = value.boundVars
  }
}

/** The base of all Orctimizer expressions.
  *
  * Expressions can be executed on their own and publish value. All subclasses
  * are documented with their execution semantics.
  */
@branch @replacement[Expression]
sealed abstract class Expression
  extends NamedAST
  with NamedInfixCombinators
  with Substitution[Expression]
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
        case x: BoundVar.Z => (if (x.contextBoundVars contains x.value) {} else { varset += x.value }); x.value
      }
    }
    collect(this.toZipper())
    Set.empty ++ varset
  }
}

object Expression {
  class Z {
    /** @return the set of all variable bound in the current context.
      */
    def contextBoundVars = {
      parents.flatMap(_.value.boundVars)
    }

    /** @return the set of free variable in this expression.
      *
      * This simply forwards to value.freeVars.
      */
    def freeVars = {
      value.freeVars
    }
  }
}

/** Publish nothing and halts immediately.
  */
@leaf @transform
final case class Stop() extends Expression

/** Publish a future and execute `expr` resolve the future to it's first publication.
  */
@leaf @transform
final case class Future(@subtree expr: Expression) extends Expression

/** Force a set of futures, halting if any halts.
  *
  * `expr` only executes when all the futures (`vs`) are resolved to values. `xs` are bound to the values of
  * the futures.
  */
@leaf @transform
final case class Force(xs: Seq[BoundVar], @subtree vs: Seq[Argument], @subtree expr: Expression) extends Expression {
  require(!xs.isInstanceOf[collection.TraversableView[_, _]])
  require(!vs.isInstanceOf[collection.TraversableView[_, _]])
  override def boundVars: Set[BoundVar] = xs.toSet
}
object Force {
  /** Construct a Force node with only one future and value.
    */
  def apply(x: BoundVar, v: Argument, expr: Expression): Force =
    Force(List(x), List(v), expr)

  /** Construct an expression which forces the value `v` and publishes the result.
    */
  def asExpr(v: Argument) = {
    val x = new BoundVar()
    Force(List(x), List(v), x)
  }
}

/** Wait for all `futures` to be resolved to a value or stop, then publish `expr`.
  *
  * `expr` must be a method. This allows for rewritting concrete values in place of the futures.
  */
@leaf @transform
final case class Resolve(@subtree futures: Seq[Argument], @subtree expr: Argument) extends Expression {
  require(!futures.isInstanceOf[collection.TraversableView[_, _]])
}

/** Publish a value (generally a method or method future) which should be called to handle a call to `expr`.
  *
  * `expr` may not be a future. The expression must always publish a value. The published value may be a future,
  * but only if `expr` is not a method.
  *
  * This node exists to allow objects to be translated into methods before the IfLenientMethod check. This
  * is required to correctly implement dynamic .apply methods.
  */
@leaf @transform
final case class GetMethod(@subtree target: Argument) extends Expression

/** If `v` is a lenient method (a routine), execute `left` otherwise execute `right`.
  *
  * If `v` is not a method of any kind, then this will execute `right`. For instance,
  * objects (even those with a .apply field) will cause `right` to execute.
  */
@leaf @transform
final case class IfLenientMethod(@subtree v: Argument, @subtree left: Expression, @subtree right: Expression) extends Expression

/** Call `target` with arguments `args`.
  *
  * This can call all kinds of methods (routine or service, and internal or external).
  */
@leaf @transform
final case class Call(@subtree target: Argument, @subtree args: Seq[Argument], @subtree typeargs: Option[Seq[Type]]) extends Expression {
  require(!args.isInstanceOf[collection.TraversableView[_, _]])
}

/** Execute `left` and `right` concurrently.
  */
@leaf @transform
final case class Parallel(@subtree left: Expression, @subtree right: Expression) extends Expression

/** Execute `left` and whenever it publishes, execute an instance of `right` with `x` bound to the publication.
  */
@leaf @transform
final case class Branch(@subtree left: Expression, x: BoundVar, @subtree right: Expression) extends Expression
  with hasOptionalVariableName {
  transferOptionalVariableName(x, this)

  override def boundVars: Set[BoundVar] = Set(x)
}

object Branch {
  /** A smart constructor which normalizes the AST to be right nested for Branch and things that contain it.
   */
  def apply(left: Expression, x: BoundVar, right: Expression): Expression = {
    left match {
      case Force(xs, vs, b) =>
        Force(xs, vs, Branch(b, x, right))
      case DeclareMethods(ms, b) =>
        DeclareMethods(ms, Branch(b, x, right))
      case DeclareType(t, tt, b) =>
        DeclareType(t, tt, Branch(b, x, right))
      case Branch(l, y, r) =>
        Branch(l, y, Branch(r, x, right))
      case _ =>
        new Branch(left, x, right)
    }
  }
}

/** Execute `expr` and terminate it when it publishes for the first time.
  */
@leaf @transform
final case class Trim(@subtree expr: Expression) extends Expression

/** Execute `left` and if it halts without publishing a value, execute `right`.
  */
@leaf @transform
final case class Otherwise(@subtree left: Expression, @subtree right: Expression) extends Expression

object Otherwise {
  /** A smart constructor which normalizes the AST to be right nested for Otherwise and things that contain it.
   */
  def apply(left: Expression, right: Expression): Expression = {
    left match {
      case DeclareType(t, tt, b) =>
        DeclareType(t, tt, Otherwise(b, right))
      case Otherwise(l, r) =>
        Otherwise(l, Otherwise(r, right))
      case _ =>
        new Otherwise(left, right)
    }
  }
}

/** Declare a set of recursive methods, and execute `body`.
  *
  * The methods will captures their closures at this point. All closures are lenient.
  * Methods bind their names to method values. To regain routine strictness semantics,
  * Resolve must be used to wait for closed variables to be resolved.
  */
@leaf @transform
final case class DeclareMethods(@subtree methods: Seq[Method], @subtree body: Expression) extends Expression {
  override def boundVars: Set[BoundVar] = methods.map(_.name).toSet
}

/** A static representation of a method.
  *
  * In Orctimizer, all method are lenient on bound variables and parameters. The Orc
  * semantics are reclaimed using Force and Resove.
  *
  * These objects are used in DeclareMethods.
  */
@branch @replacement[Method]
sealed abstract class Method
  extends NamedAST
  with hasOptionalVariableName
  with Substitution[Method]
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
    returntype: Option[Type] = returntype): Method

  override def boundVars: Set[BoundVar] = formals.toSet
}

object Method {
  def unapply(value: Method) = {
    if (value != null) {
      Some((value.name, value.formals, value.body, value.typeformals, value.argtypes, value.returntype))
    } else {
      None
    }
  }

  class Z {
    def name = this match {
      case Routine.Z(name, formals, body, typeformals, argtypes, returntype) =>
        name
      case Service.Z(name, formals, body, typeformals, argtypes, returntype) =>
        name
    }
    def formals = this match {
      case Routine.Z(name, formals, body, typeformals, argtypes, returntype) =>
        formals
      case Service.Z(name, formals, body, typeformals, argtypes, returntype) =>
        formals
    }
    def body = this match {
      case Routine.Z(name, formals, body, typeformals, argtypes, returntype) =>
        body
      case Service.Z(name, formals, body, typeformals, argtypes, returntype) =>
        body
    }
  }
  object Z {
    def unapply(value: Method.Z) = {
      value match {
        case Routine.Z(name, formals, body, typeformals, argtypes, returntype) =>
          Some((name, formals, body, typeformals, argtypes, returntype))
        case Service.Z(name, formals, body, typeformals, argtypes, returntype) =>
          Some((name, formals, body, typeformals, argtypes, returntype))
        case _ =>
          None
      }
    }
  }
}

/** A routine method.
  *
  * In Orctimizer, routines are simply methods which execute in the terminator scope of the call and allow the declaration to halt.
  */
@leaf @transform
final case class Routine(override val name: BoundVar, override val formals: Seq[BoundVar], @subtree override val body: Expression, typeformals: Seq[BoundTypevar], @subtree argtypes: Option[Seq[Type]], @subtree returntype: Option[Type])
  extends Method {
  require(!formals.isInstanceOf[collection.TraversableView[_, _]])
  require(!typeformals.isInstanceOf[collection.TraversableView[_, _]])
  require(!argtypes.isInstanceOf[collection.TraversableView[_, _]])

  transferOptionalVariableName(name, this)

  def copy(name: BoundVar = name,
    formals: Seq[BoundVar] = formals,
    body: Expression = body,
    typeformals: Seq[BoundTypevar] = typeformals,
    argtypes: Option[Seq[Type]] = argtypes,
    returntype: Option[Type] = returntype) = {
    this ->> Routine(name, formals, body, typeformals, argtypes, returntype)
  }
}

/** A service method.
  *
  * In Orctimizer, services are simply methods which execute in the terminator scope of the declaration and prevent the declaration from halting.
  */
@leaf @transform
final case class Service(override val name: BoundVar, override val formals: Seq[BoundVar], @subtree override val body: Expression, typeformals: Seq[BoundTypevar], @subtree argtypes: Option[Seq[Type]], @subtree returntype: Option[Type])
  extends Method {
  require(!formals.isInstanceOf[collection.TraversableView[_, _]])
  require(!typeformals.isInstanceOf[collection.TraversableView[_, _]])
  require(!argtypes.isInstanceOf[collection.TraversableView[_, _]])
  
  transferOptionalVariableName(name, this)

  def copy(name: BoundVar = name,
    formals: Seq[BoundVar] = formals,
    body: Expression = body,
    typeformals: Seq[BoundTypevar] = typeformals,
    argtypes: Option[Seq[Type]] = argtypes,
    returntype: Option[Type] = returntype) = {
    this ->> Service(name, formals, body, typeformals, argtypes, returntype)
  }
}

/** Construct a recursive object with specified fields and bindings.
  *
  * Field values are specified using a special type to guarantee that every field value
  * is immediately available.
  */
@leaf @transform
final case class New(self: BoundVar, @subtree selfType: Option[Type], @subtree bindings: Map[values.Field, FieldValue], @subtree objType: Option[Type]) extends Expression {
  require(!bindings.isInstanceOf[collection.TraversableView[_, _]])
  override def boundVars: Set[BoundVar] = Set(self)
}

/** The base for field trees in objects.
  *
  * Field values use a special type to guarantee that every field value
  * is immediately available.
  */
@branch @replacement[FieldValue]
sealed abstract class FieldValue extends NamedAST with PrecomputeHashcode {
  this: Product =>
}

/** A field which is bound to a future which will be resolved to the first publication of `expr`.
  *
  * This is equivelent to future { `expr` }.
  */
@leaf @transform
final case class FieldFuture(@subtree expr: Expression) extends FieldValue

/** A field which is bound to a concrete value.
  *
  * The value may be a future.
  */
@leaf @transform
final case class FieldArgument(@subtree expr: Argument) extends FieldValue

/** Publish the `field` from the value `obj`.
  *
  * Halt without publishing, if `obj` does not have such a field.
  */
@leaf @transform
final case class GetField(@subtree obj: Argument, field: values.Field) extends Expression

/** The base for simple expressions which always immediately produce a value.
  */
@branch
sealed abstract class Argument extends Expression {
  this: Product =>
}

/** Publish `constantValue` immediately.
  */
@leaf @transform
final case class Constant(constantValue: AnyRef) extends Argument

/** Publish the value of the variable.
  */
@branch
sealed abstract class Var extends Argument with hasOptionalVariableName {
  this: Product =>
}

/** A variable that was never declared.
  *
  * Encountering one of these is generally an error.
  */
@leaf @transform
final case class UnboundVar(name: String) extends Var {
  optionalVariableName = Some(name)
}

/** A variable bound to some value in the context of this expression.
  *
  * BoundVars use identity equality without regard for their name.
  */
@leaf @transform
final class BoundVar(val optionalName: Option[String] = None) extends Var with hasAutomaticVariableName with Product {
  // Members declared in scala.Equals
  def canEqual(that: Any): Boolean = that.isInstanceOf[BoundVar]

  // Members declared in scala.Product
  def productArity: Int = 1
  def productElement(n: Int): Any = optionalName

  optionalVariableName = optionalName
  //autoName("ov")
}

/** Declare a type in a context.
  */
@leaf @transform
final case class DeclareType(name: BoundTypevar, @subtree t: Type, @subtree body: Expression) extends Expression
  with hasOptionalVariableName { transferOptionalVariableName(name, this) }

/** Specify that a given expression must have a specific type.
  */
@leaf @transform
final case class HasType(@subtree body: Expression, @subtree expectedType: Type) extends Expression

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

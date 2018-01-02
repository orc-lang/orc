//
// PorcAST.scala -- Scala class and objects for the Porc AST
// Project OrcScala
//
// Created by amp on May 27, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import java.io.IOException

import orc.ast.{ ASTForSwivel, ASTWithIndex, PrecomputeHashcode, hasAutomaticVariableName, hasOptionalVariableName }
import orc.util.Ternary
import orc.values.Field

import swivel.{ TransformFunction, branch, leaf, root, subtree }

abstract class Transform extends TransformFunction {
  def onExpression: PartialFunction[Expression.Z, Expression] = {
    case a: Argument.Z if onArgument.isDefinedAt(a) => onArgument(a)
  }
  def apply(e: Expression.Z) = transformWith[Expression.Z, Expression](e)(this, onExpression)

  def onArgument: PartialFunction[Argument.Z, Argument] = PartialFunction.empty
  def apply(e: Argument.Z) = transformWith[Argument.Z, Argument](e)(this, onArgument)

  def onMethod: PartialFunction[Method.Z, Method] = PartialFunction.empty
  def apply(e: Method.Z) = transformWith[Method.Z, Method](e)(this, onMethod)

  def apply(e: PorcAST.Z): PorcAST = e match {
    case e: Argument.Z => apply(e)
    case e: Expression.Z => apply(e)
    case e: Method.Z => apply(e)
  }
}

/** @author amp
  */
@root @transform[Transform]
sealed abstract class PorcAST extends ASTForSwivel with Product {
  def prettyprint() = (new PrettyPrint()).reduce(this).toString()
  def prettyprintWithoutNested() = (new PrettyPrint(false)).reduce(this).toString()
  override def toString() = prettyprint()

  def boundVars: Set[Variable] = Set()

  override def transferMetadata[T <: PorcAST](e: T): T = {
    this ->> e
  }
}

// ==================== CORE ===================
@branch @replacement[Expression]
sealed abstract class Expression extends PorcAST with FreeVariables with Substitution[Expression] with PrecomputeHashcode with PorcInfixExpr

object Expression {
  class Z {
    def contextBoundVars = {
      parents.flatMap(_.value.boundVars)
    }
    def freeVars = {
      value.freeVars
    }
    def binderOf(x: Variable) = {
      parents.find(_.value.boundVars.contains(x))
    }
  }
}

@branch
sealed abstract class Argument extends Expression with PorcInfixValue with PrecomputeHashcode
@leaf @transform
final case class Constant(v: AnyRef) extends Argument {
  @throws[IOException]
  private def writeObject(out: java.io.ObjectOutputStream): Unit = {
    out.writeObject(orc.ast.oil.xml.OrcXML.anyToXML(v))
  }

  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: java.io.ObjectInputStream): Unit = {
    Constant.setVDuringDeserialize(this, orc.ast.oil.xml.OrcXML.anyRefFromXML(in.readObject().asInstanceOf[xml.Node]))
  }
}
object Constant {
  private val vField = classOf[Constant].getDeclaredField("v")
  vField.setAccessible(true)
  private def setVDuringDeserialize(c: Constant, v: AnyRef) = {
    vField.set(c, v)
  }
}

@leaf @transform
final case class PorcUnit() extends Argument
@leaf @transform
final class Variable(val optionalName: Option[String] = None) extends Argument with hasAutomaticVariableName with Serializable {
  optionalVariableName = optionalName
  autoName("pv")

  def this(s: String) = this(Some(s))

  override def productIterator = Nil.iterator
  // Members declared in scala.Equals
  def canEqual(that: Any): Boolean = that.isInstanceOf[Variable]
  // Members declared in scala.Product
  def productArity: Int = 0
  def productElement(n: Int): Any = ???

  override val hashCode = System.identityHashCode(this)
}

@leaf @transform
final case class CallContinuation(@subtree target: Argument, @subtree arguments: Seq[Argument]) extends Expression {
  require(!arguments.isInstanceOf[collection.TraversableView[_, _]])
}

@leaf @transform
final case class Let(x: Variable, @subtree v: Expression, @subtree body: Expression) extends Expression with hasOptionalVariableName {
  override def boundVars: Set[Variable] = Set(x)

  transferOptionalVariableName(x, this)
  // Set the name of the value to be the same as this. (HACK? This is a bit odd, but I think it makes sense.)
  transferOptionalVariableName(this, v)
}

@leaf @transform
final case class Sequence(@subtree es: Seq[Expression]) extends Expression {
  require(!es.isInstanceOf[collection.TraversableView[_, _]])
}
object Sequence {
  def apply(es: Seq[Expression]): Expression = {
    val es1 = (es.flatMap {
      case Sequence(fs) => fs
      case PorcUnit() => Seq()
      case e => Seq(e)
    }).toVector

    es1.size match {
      case 0 =>
        PorcUnit()
      case 1 =>
        es1.head
      case _ =>
        new Sequence(es1)
    }
  }
}

@leaf @transform
final case class Continuation(arguments: Seq[Variable], @subtree body: Expression) extends Expression with hasOptionalVariableName with ASTWithIndex {
  require(!arguments.isInstanceOf[collection.TraversableView[_, _]])
  override def boundVars: Set[Variable] = arguments.toSet

  @throws[IOException]
  private def writeObject(out: java.io.ObjectOutputStream): Unit = {
    out.defaultWriteObject()
    out.writeObject(optionalIndex)
  }

  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: java.io.ObjectInputStream): Unit = {
    in.defaultReadObject()
    optionalIndex = in.readObject().asInstanceOf[Option[Int]]
  }
}

@leaf @transform
final case class MethodDeclaration(@subtree t: Argument, @subtree defs: Seq[Method], @subtree body: Expression) extends Expression {
  override def boundVars: Set[Variable] = defs.map(_.name).toSet
}

@branch @replacement[Method]
sealed abstract class Method extends PorcAST with hasOptionalVariableName with ASTWithIndex with Serializable {
  def name: Variable
  def isRoutine: Boolean
  def arguments: Seq[Variable]
  def body: Expression

  def allArguments: Seq[Variable]

  override def boundVars: Set[Variable] = allArguments.toSet

  transferOptionalVariableName(name, this)

  @throws[IOException]
  private def writeObject(out: java.io.ObjectOutputStream): Unit = {
    out.defaultWriteObject()
    out.writeObject(optionalIndex)
  }

  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: java.io.ObjectInputStream): Unit = {
    in.defaultReadObject()
    optionalIndex = in.readObject().asInstanceOf[Option[Int]]
  }
}

object Method {
  def unapply(m: Method): Option[(Variable, Boolean, Seq[Variable], Expression)] = m match {
    case MethodCPS(n, _, _, _, i, a, b) =>
      Some((n, i, a, b))
    case MethodDirect(n, i, a, b) =>
      Some((n, i, a, b))
    case _ =>
      None
  }

  class Z {
    def name: Variable
    def isRoutine: Boolean
    def arguments: Seq[Variable]
    def allArguments: Seq[Variable] = value.allArguments
    def body: Expression.Z
  }

  object Z {
    def unapply(m: Method.Z): Option[(Variable, Boolean, Seq[Variable], Expression.Z)] = m match {
      case MethodCPS.Z(n, _, _, _, i, a, b) =>
        Some((n, i, a, b))
      case MethodDirect.Z(n, i, a, b) =>
        Some((n, i, a, b))
      case _ =>
        None
    }
  }
}

@leaf @transform
final case class MethodCPS(name: Variable, pArg: Variable, cArg: Variable, tArg: Variable, isRoutine: Boolean, arguments: Seq[Variable], @subtree body: Expression) extends Method {
  require(!arguments.isInstanceOf[collection.TraversableView[_, _]])
  override def allArguments: Seq[Variable] = pArg +: cArg +: tArg +: arguments
}
@leaf @transform
final case class MethodDirect(name: Variable, isRoutine: Boolean, arguments: Seq[Variable], @subtree body: Expression) extends Method {
  require(!arguments.isInstanceOf[collection.TraversableView[_, _]])
  override def allArguments: Seq[Variable] = arguments
}

/** Call a CPS method.
  *
  * This consumes a token of c when it is called and then returns a token to each call to p which must return that token to its caller.
  *
  * This call will halt if `target` is killed after discorporating.
  */
@leaf @transform
final case class MethodCPSCall(isExternal: Ternary, @subtree target: Argument, @subtree p: Argument, @subtree c: Argument, @subtree t: Argument, @subtree arguments: Seq[Argument]) extends Expression with ASTWithIndex {
  require(!arguments.isInstanceOf[collection.TraversableView[_, _]])

  @throws[IOException]
  private def writeObject(out: java.io.ObjectOutputStream): Unit = {
    out.defaultWriteObject()
    out.writeObject(optionalIndex)
  }

  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: java.io.ObjectInputStream): Unit = {
    in.defaultReadObject()
    optionalIndex = in.readObject().asInstanceOf[Option[Int]]
  }
}
@leaf @transform
final case class MethodDirectCall(isExternal: Ternary, @subtree target: Argument, @subtree arguments: Seq[Argument]) extends Expression with ASTWithIndex {
  require(!arguments.isInstanceOf[collection.TraversableView[_, _]])
}

@leaf @transform
final case class IfLenientMethod(@subtree argument: Argument, @subtree left: Expression, @subtree right: Expression) extends Expression

@leaf @transform
final case class GetField(@subtree argument: Argument, field: Field) extends Expression

@leaf @transform
final case class GetMethod(@subtree argument: Argument) extends Expression

@leaf @transform
final case class New(@subtree bindings: Map[Field, Argument]) extends Expression

// ==================== PROCESS ===================

/** Spawn a new task.
  *
  * This consumes a token of c and passes it to computation.
  */
@leaf @transform
final case class Spawn(@subtree c: Argument, @subtree t: Argument, blockingComputation: Boolean, @subtree computation: Argument) extends Expression

@leaf @transform
final case class NewTerminator(@subtree parentT: Argument) extends Expression
@leaf @transform
final case class Kill(@subtree c: Argument, @subtree t: Argument, @subtree continuation: Argument) extends Expression
@leaf @transform
final case class CheckKilled(@subtree t: Argument) extends Expression

/** Create a counter.
  *
  * The counter initially has one token.
  *
  * This consumes a token of parentC and then returns it to the halt handler.
  */
@branch
sealed abstract class NewCounter extends Expression {
}

object NewCounter {
  def unapply(o: NewCounter): Boolean = {
    o != null
  }

  class Z {
  }

  object Z {
    def unapply(o: NewCounter.Z): Boolean = {
      o != null
    }
  }
}

@leaf @transform
final case class NewSimpleCounter(@subtree parentC: Argument, @subtree haltHandler: Argument) extends NewCounter
@leaf @transform
final case class NewServiceCounter(@subtree callingC: Argument, @subtree containingC: Argument, @subtree t: Argument) extends NewCounter
@leaf @transform
final case class NewTerminatorCounter(@subtree parentC: Argument, @subtree t: Argument) extends NewCounter

/** Add a new token to a counter.
  */
@leaf @transform
final case class NewToken(@subtree c: Argument) extends Expression
/** Remove a token from a counter.
  */
@leaf @transform
final case class HaltToken(@subtree c: Argument) extends Expression
@leaf @transform
final case class SetDiscorporate(@subtree c: Argument) extends Expression

/** Catch either kill or halted.
  *
  */
@leaf @transform
final case class TryOnException(@subtree body: Expression, @subtree handler: Expression) extends Expression
@leaf @transform
final case class TryFinally(@subtree body: Expression, @subtree handler: Expression) extends Expression

// ==================== FUTURE ===================

@leaf @transform
final case class NewFuture(raceFreeResolution: Boolean) extends Expression
@leaf @transform
final case class Bind(@subtree future: Argument, @subtree v: Argument) extends Expression
@leaf @transform
final case class BindStop(@subtree future: Argument) extends Expression
/** Force a sequence of futures and pass their values to p.
  *
  * If any future halts this whole call halts.
  *
  * This consumes a token of c and passes it to p and finally removes it.
  */
@leaf @transform
final case class Force(@subtree p: Argument, @subtree c: Argument, @subtree t: Argument, @subtree futures: Seq[Argument]) extends Expression {
  require(!futures.isInstanceOf[collection.TraversableView[_, _]])
}
/** Resolve a sequence of futures.
  *
  * This calls p when every future is either stopped or resolved to a value.
  *
  * This consumes a token of c and passes it to p and finally removes it.
  */
@leaf @transform
final case class Resolve(@subtree p: Argument, @subtree c: Argument, @subtree t: Argument, @subtree futures: Seq[Argument]) extends Expression {
  require(!futures.isInstanceOf[collection.TraversableView[_, _]])
}

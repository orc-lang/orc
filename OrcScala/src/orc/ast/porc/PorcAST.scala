//
// PorcAST.scala -- Scala class/trait/object PorcAST
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

import orc.ast.AST
import orc.ast.hasOptionalVariableName
import orc.values.sites
import orc.ast.PrecomputeHashcode
import java.util.logging.Level
import orc.values.Field
import orc.ast.ASTForSwivel
import swivel.root
import swivel.replacement
import swivel.leaf
import swivel.branch
import swivel.subtree
import orc.ast.hasAutomaticVariableName
import scala.PartialFunction
import swivel.TransformFunction
import orc.util.Ternary

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
  override def toString() = prettyprint()

  def boundVars: Set[Variable] = Set()
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
final case class Constant(v: AnyRef) extends Argument
@leaf @transform
final case class PorcUnit() extends Argument
@leaf @transform
final class Variable(val optionalName: Option[String] = None) extends Argument with hasAutomaticVariableName {
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
final case class CallContinuation(@subtree target: Argument, @subtree arguments: Seq[Argument]) extends Expression

@leaf @transform
final case class Let(x: Variable, @subtree v: Expression, @subtree body: Expression) extends Expression {
  override def boundVars: Set[Variable] = Set(x)
}

@leaf @transform
final case class Sequence(@subtree es: Seq[Expression]) extends Expression {
  //assert(!es.isEmpty)

  def simplify = es match {
    case Seq(e) => e
    case _ => this
  }
}
object Sequence {
  def apply(es: Seq[Expression]): Sequence = {
    new Sequence((es.flatMap {
      case Sequence(fs) => fs
      case PorcUnit() => Seq()
      case e => Seq(e)
    }).toList)
  }
}

@leaf @transform
final case class Continuation(arguments: Seq[Variable], @subtree body: Expression) extends Expression {
  override def boundVars: Set[Variable] = arguments.toSet
}

@leaf @transform
final case class MethodDeclaration(@subtree defs: Seq[Method], @subtree body: Expression) extends Expression {
  override def boundVars: Set[Variable] = defs.map(_.name).toSet
}

@branch @replacement[Method]
sealed abstract class Method extends PorcAST {
  def name: Variable
  def arguments: Seq[Variable]
  def body: Expression
  def areArgsLenient: Boolean

  def allArguments: Seq[Variable]

  override def boundVars: Set[Variable] = allArguments.toSet
}

object Method {
  class Z {
    def name: Variable
    def arguments: Seq[Variable]
    def allArguments: Seq[Variable] = value.allArguments
    def body: Expression.Z
  }
}

@leaf @transform
final case class MethodCPS(name: Variable, pArg: Variable, cArg: Variable, tArg: Variable, areArgsLenient: Boolean, arguments: Seq[Variable], @subtree body: Expression) extends Method {
  override def allArguments: Seq[Variable] = pArg +: cArg +: tArg +: arguments
}
@leaf @transform
final case class MethodDirect(name: Variable, areArgsLenient: Boolean, arguments: Seq[Variable], @subtree body: Expression) extends Method {
  override def allArguments: Seq[Variable] = arguments
}

/** Call a CPS method.
  *
  * This consumes a token of c when it is called and then returns a token to each call to p which must return that token to its caller.
  */
@leaf @transform
final case class MethodCPSCall(isExternal: Ternary, @subtree target: Argument, @subtree p: Argument, @subtree c: Argument, @subtree t: Argument, @subtree arguments: Seq[Argument]) extends Expression
@leaf @transform
final case class MethodDirectCall(isExternal: Ternary, @subtree target: Argument, @subtree arguments: Seq[Argument]) extends Expression

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
final case class Kill(@subtree t: Argument) extends Expression
@leaf @transform
final case class CheckKilled(@subtree t: Argument) extends Expression

/** Create a counter.
  *
  * The counter initially has one token.
  *
  * This consumes a token of parentC and then returns it to the halt handler.
  */
@leaf @transform
final case class NewCounter(@subtree parentC: Argument, @subtree haltHandler: Argument) extends Expression
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
final case class NewFuture() extends Expression
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
final case class Force(@subtree p: Argument, @subtree c: Argument, @subtree t: Argument, @subtree futures: Seq[Argument]) extends Expression
/** Resolve a sequence of futures.
  *
  * This calls p when every future is either stopped or resolved to a value.
  *
  * This consumes a token of c and passes it to p and finally removes it.
  */
@leaf @transform
final case class Resolve(@subtree p: Argument, @subtree c: Argument, @subtree t: Argument, @subtree futures: Seq[Argument]) extends Expression

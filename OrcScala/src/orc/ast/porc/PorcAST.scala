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

/** @author amp
  */
sealed abstract class PorcAST extends AST with Product with WithContextInfixCombinator {
  def prettyprint() = (new PrettyPrint()).reduce(this).toString()
  override def toString() = prettyprint()

  var number: Option[Int] = None

  override def subtrees: Iterable[AST] = this match {
    case Call(a, b) => List(a, b)
    case Let(a, b, c) => List(a, b, c)
    case Sequence(l) => l
    case Continuation(x, e) => List(x, e)
    case DefDeclaration(ds, e) => e +: ds
    case DefCPS(a, b, c, d, e, f) => a +: b +: c +: d +: f +: e
    case DefDirect(a, b, c) => a +: c +: b
    case SiteCall(a, b, c, d, e) => a +: c +: b +: d +: e
    case SiteCallDirect(a, b) => a +: b
    case DefCall(a, b, c, d, e) => a +: c +: b +: d +: e
    case DefCallDirect(a, b) => a +: b
    case IfDef(a, b, c) => List(a, b, c)
    case New(b) => b.values.toList
    case Spawn(a, b, c) => List(a, b, c)
    case NewTerminator(a) => List(a)
    case Kill(a) => List(a)
    case TryOnKilled(a, b) => List(a, b)
    case NewCounter(a, b) => List(a, b)
    case Halt(a) => List(a)
    case TryOnHalted(a, b) => List(a, b)
    case TryFinally(a, b) => List(a, b)
    case NewFuture() => List()
    case SpawnBindFuture(a, b, c, d, e, f) => List(a, b, c, d, e, f)
    case Force(a, b, c, _, e) => a +: b +: c +: e
    case GetField(a, b, c, d, _) => List(a, b, c, d)
    case TupleElem(a, _) => List(a)
    case _ => List()
  }

  /** Assign numbers in depth first order stating at 0.
    */
  def assignNumbers() {
    assignNumbersStartingAt(0)
  }

  /** Assign numbers starting at n and returning the
    */
  protected[porc] def assignNumbersStartingAt(n: Int): Int = {
    /*if (Logger.isLoggable(Level.FINE)) {
      number match {
        case Some(oldN) =>
          if (n != oldN)
            Logger.fine("Reassigning PorcAST instruction number to something different. This may be a problem or at least confusing.")
        case None => ()
      }
    }*/
    number = Some(n)

    val children = subtrees.collect { case c: PorcAST => c }.toSeq
    children.reverse.foldRight(n + 1)(_.assignNumbersStartingAt(_))
  }
}

sealed trait UnnumberedPorcAST extends PorcAST {
  override protected[porc] def assignNumbersStartingAt(n: Int): Int = {
    val children = subtrees.collect { case c: PorcAST => c }.toSeq
    children.reverse.foldRight(n)(_.assignNumbersStartingAt(_))
  }
}

// ==================== CORE ===================
sealed abstract class Expr extends PorcAST with FreeVariables with Substitution[Expr] with Product with PrecomputeHashcode with PorcInfixExpr

sealed abstract class Value extends Expr with PorcInfixValue with UnnumberedPorcAST with PrecomputeHashcode //with Substitution[Value]
case class OrcValue(value: AnyRef) extends Value
/*case class Tuple(values: Seq[Value]) extends Value
object Tuple {
  def apply(values: Value*): Tuple = Tuple(values.toList)
  def unapplySeq(t: Tuple): Option[Seq[Value]] = Some(t.values)
}*/

case class Unit() extends Value

class Var(optionalName: Option[String] = None) extends Value with hasOptionalVariableName {
  optionalVariableName = optionalName match {
    case Some(n) => Some(n)
    case None =>
      Some(Var.getNextVariableName())
  }

  def this(s: String) = this(Some(Var.getNextVariableName(s)))

  override def productIterator = Nil.iterator
  // Members declared in scala.Equals
  def canEqual(that: Any): Boolean = that.isInstanceOf[Var]
  // Members declared in scala.Product
  def productArity: Int = 0
  def productElement(n: Int): Any = ???

  override val hashCode = System.identityHashCode(this)
}

object Var {
  private var nextVar: Int = 0
  def getNextVariableName(): String = getNextVariableName("pv")
  def getNextVariableName(s: String): String = synchronized {
    nextVar += 1
    s"`$s$nextVar"
  }
}

case class Call(target: Value, argument: Value) extends Expr
case class Let(x: Var, v: Expr, body: Expr) extends Expr

case class Sequence(es: Seq[Expr]) extends Expr with UnnumberedPorcAST {
  //assert(!es.isEmpty)

  def simplify = es match {
    case Seq(e) => e
    case _ => this
  }
}
object Sequence {
  def apply(es: Seq[Expr]): Sequence = {
    new Sequence((es.flatMap {
      case Sequence(fs) => fs
      case e => Seq(e)
    }).toList)
  }
  /*def apply(es: Expr*): Sequence = {
    new Sequence((es.flatMap {
      case Sequence(fs) => fs
      case e => Seq(e)
    }).toList)
  }*/
}

case class Continuation(argument: Var, body: Expr) extends Expr

case class DefDeclaration(defs: Seq[Def], body: Expr) extends Expr
sealed abstract class Def extends PorcAST {
  def name: Var
  def arguments: Seq[Var]
  def body: Expr

  def allArguments: Seq[Var] = arguments
}
final case class DefCPS(name: Var, pArg: Var, cArg: Var, tArg: Var, arguments: Seq[Var], body: Expr) extends Def {
  override def allArguments: Seq[Var] = pArg +: cArg +: tArg +: arguments
}
final case class DefDirect(name: Var, arguments: Seq[Var], body: Expr) extends Def

case class SiteCall(target: Value, pArg: Value, cArg: Value, tArg: Value, arguments: Seq[Value]) extends Expr
case class SiteCallDirect(target: Value, arguments: Seq[Value]) extends Expr
case class DefCall(target: Value, pArg: Value, cArg: Value, tArg: Value, arguments: Seq[Value]) extends Expr
case class DefCallDirect(target: Value, arguments: Seq[Value]) extends Expr

case class IfDef(argument: Value, left: Expr, right: Expr) extends Expr

case class New(bindings: Map[Field, Expr]) extends Expr

// ==================== PROCESS ===================

// TODO: The semantics of this have been changed to "spawn or run as the runtime prefers"
case class Spawn(cArg: Value, tArg: Value, body: Expr) extends Expr

case class NewTerminator(parent: Value) extends Expr
case class Kill(t: Value) extends Expr
case class TryOnKilled(body: Expr, handler: Expr) extends Expr

case class NewCounter(parent: Value, haltCont: Expr) extends Expr
case class Halt(c: Value) extends Expr
case class SetDiscorporate(c: Value) extends Expr
case class TryOnHalted(body: Expr, handler: Expr) extends Expr

case class TryFinally(body: Expr, handler: Expr) extends Expr

// ==================== FUTURE ===================

case class NewFuture() extends Expr

case class SpawnBindFuture(fut: Value, c: Value, t: Value, pArg: Var, cArg: Var, expr: Expr) extends Expr

case class Force(p: Value, c: Value, t: Value, forceClosures: Boolean, futures: Seq[Value]) extends Expr

case class GetField(p: Value, c: Value, t: Value, future: Value, field: Field) extends Expr

case class TupleElem(v: Value, i: Int) extends Expr

// ==================== TYPE ===================
/*
sealed abstract class Type extends PorcAST

case class ContinuationType() extends Type
case class SiteType() extends Type
case class CounterType() extends Type
case class TerminatorType() extends Type
*/

//
// PorcAST.scala -- Scala class/trait/object PorcAST

// Project OrcScala
//
// $Id$
//
// Created by amp on May 27, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
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

/**
  * @author amp
  */
sealed abstract class PorcAST extends AST with Product with WithContextInfixCombinator {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()

  var number: Option[Int] = None
  
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

trait UnnumberedPorcAST extends PorcAST {
  override protected[porc] def assignNumbersStartingAt(n: Int): Int = {
    val children = subtrees.collect { case c: PorcAST => c }.toSeq
    children.reverse.foldRight(n)(_.assignNumbersStartingAt(_))
  }
}

// ==================== CORE ===================
sealed abstract class Expr extends PorcAST with FreeVariables with Substitution[Expr] with Product with PrecomputeHashcode with PorcInfixExpr

sealed abstract class Value extends Expr with PorcInfixValue with UnnumberedPorcAST //with Substitution[Value]
case class OrcValue(value: AnyRef) extends Value
/*case class Tuple(values: List[Value]) extends Value
object Tuple {
  def apply(values: Value*): Tuple = Tuple(values.toList)
  def unapplySeq(t: Tuple): Option[List[Value]] = Some(t.values)
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

case class Sequence(es: List[Expr]) extends Expr with UnnumberedPorcAST {
  //assert(!es.isEmpty)
  
  def simplify = es match {
    case List(e) => e
    case _ => this
  }
}
object Sequence {
  /*def apply(es: Seq[Expr]): Sequence = {
    new Sequence((es.flatMap {
      case Sequence(fs) => fs
      case e => List(e)
    }).toList)
  }*/
  def apply(es: Expr*): Sequence = {
    new Sequence((es.flatMap {
      case Sequence(fs) => fs
      case e => List(e)
    }).toList)
  }
}

case class Continuation(argument: Var, body: Expr) extends Expr

case class Site(defs: List[SiteDef], body: Expr) extends Expr
sealed abstract class SiteDef extends PorcAST {
  def name: Var
  def arguments: List[Var]
  def body: Expr
  
  def allArguments: List[Var] = arguments
}
final case class SiteDefCPS(name: Var, pArg: Var, cArg: Var, tArg: Var, arguments: List[Var], body: Expr) extends SiteDef {
  override def allArguments: List[Var] = pArg :: cArg :: tArg :: arguments
}
final case class SiteDefDirect(name: Var, arguments: List[Var], body: Expr) extends SiteDef

case class SiteCall(target: Value, pArg: Value, cArg: Value, tArg: Value, arguments: List[Value]) extends Expr
case class SiteCallDirect(target: Value, arguments: List[Value]) extends Expr

// ==================== PROCESS ===================

case class Spawn(cArg: Value, tArg: Value, body: Expr) extends Expr

case class NewTerminator(parent: Value) extends Expr
case class Kill(t: Value) extends Expr
case class TryOnKilled(body: Expr, handler: Expr) extends Expr

case class NewCounter(parent: Value, haltCont: Expr) extends Expr
case class Halt(c: Value) extends Expr
case class TryOnHalted(body: Expr, handler: Expr) extends Expr

case class TryFinally(body: Expr, handler: Expr) extends Expr

// ==================== FUTURE ===================

case class SpawnFuture(c: Value, t: Value, pArg: Var, cArg: Var, expr: Expr) extends Expr
case class Force(p: Value, c: Value, future: Value, forceClosures: Boolean) extends Expr

case class GetField(p: Value, c: Value, t: Value, future: Value, field: Field) extends Expr

case class Resolve(c: Value, t: Value, future: Value, expr: Expr) extends Expr

case class ForceMany(c: Value, t: Value, futures: List[Value], args: List[Var], expr: Expr) extends Expr

// ==================== TYPE ===================
/*
sealed abstract class Type extends PorcAST

case class ContinuationType() extends Type
case class SiteType() extends Type
case class CounterType() extends Type
case class TerminatorType() extends Type
*/

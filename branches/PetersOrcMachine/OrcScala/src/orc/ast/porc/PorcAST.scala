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

/**
  *
  * @author amp
  */
sealed abstract class PorcAST extends AST with Product with WithContextInfixCombinator {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()
}

// ==================== CORE ===================
sealed abstract class Expr extends PorcAST with FreeVariables with ReferencesRegisters with Substitution[Expr] with Product with PrecomputeHashcode with PorcInfixExpr

sealed abstract class Value extends Expr with PorcInfixValue //with Substitution[Value]
case class OrcValue(value: AnyRef) extends Value
/*case class Tuple(values: List[Value]) extends Value
object Tuple {
  def apply(values: Value*): Tuple = Tuple(values.toList)
  def unapplySeq(t: Tuple): Option[List[Value]] = Some(t.values)
}*/

case class Unit() extends Value
case class Bool(b: Boolean) extends Value

class Var(optionalName: Option[String] = None) extends Value with hasOptionalVariableName {
  optionalVariableName = optionalName match {
    case Some(n) => Some(n)
    case None =>
      Some(Var.getNextVariableName())
  }

  def this(s : String) = this(Some(Var.getNextVariableName(s)))

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


case class Call(target: Value, argument: List[Value]) extends Expr
case class Let(x: Var, v: Expr, body: Expr) extends Expr
case class Sequence(es: List[Expr]) extends Expr
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

case class Lambda(arguments: List[Var], body: Expr) extends Expr

case class Site(defs: List[SiteDef], body: Expr) extends Expr
case class SiteDef(name: Var, arguments: List[Var], pArg: Var, body: Expr) extends PorcAST

case class SiteCall(target: Value, arguments: List[Value], pArg: Value) extends Expr
case class DirectSiteCall(target: Value, arguments: List[Value]) extends Expr

//case class Project(n: Int, v: Value) extends Expr
case class If(b: Value, thn: Expr, els: Expr) extends Expr

// ==================== PROCESS ===================

case class Spawn(target: Var) extends Expr

case class NewCounter(e: Expr) extends Expr
//TODO: case class NewCounterDisconnected(k: Command) extends Command with hasSimpleContinuation
case class DecrCounter() extends Expr
case class RestoreCounter(zeroBranch: Expr, nonzeroBranch: Expr) extends Expr
case class SetCounterHalt(haltCont: Var) extends Expr
case class CallCounterHalt() extends Expr
case class CallParentCounterHalt() extends Expr

case class NewTerminator(e: Expr) extends Expr
case class GetTerminator() extends Expr
case class SetKill() extends Expr
case class Killed() extends Expr
case class CheckKilled() extends Expr
case class TryOnKilled(body: Expr, handler: Expr) extends Expr

case class Halted() extends Expr
case class TryOnHalted(body: Expr, handler: Expr) extends Expr

case class AddKillHandler(t: Value, v: Value) extends Expr
case class CallKillHandlers() extends Expr

// ==================== FUTURE ===================

case class NewFuture() extends Expr
case class Force(futures: List[Value], bound: Value) extends Expr
case class Bind(future: Var, value: Value) extends Expr
case class Stop(future: Var) extends Expr

// ==================== FLAG ===================

case class NewFlag() extends Expr
case class SetFlag(flag: Var) extends Expr
case class ReadFlag(flag: Var) extends Expr

// ==================== EXT ====================

case class ExternalCall(site: sites.Site, arguments: List[Value], p: Value) extends Expr

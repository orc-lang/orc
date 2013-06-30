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

/**
  *
  * @author amp
  */
sealed abstract class PorcAST extends AST with WithContextInfixCombinator {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()
}

trait hasSimpleContinuation {
  val k: Command
}

// ==================== CORE ===================
sealed abstract class Value extends PorcAST with PorcInfixValue with Substitution[Value]
case class Constant(value: AnyRef) extends Value
case class Tuple(values: List[Value]) extends Value
object Tuple {
  def apply(values: Value*): Tuple = Tuple(values.toList)
}

sealed abstract class Var(optionalName: Option[String]) extends Value with hasOptionalVariableName {
  optionalVariableName = optionalName match {
    case Some(n) => Some(n)
    case None =>
      Some(Var.getNextVariableName())
  }

  def productIterator = optionalVariableName.toList.iterator
}
object Var {
  private var nextVar: Int = 0
  def getNextVariableName(): String = getNextVariableName("pv")
  def getNextVariableName(s: String): String = synchronized {
    nextVar += 1
    s"`$s$nextVar"
  }
}

class Variable(optionalName: Option[String] = None) extends Var(optionalName) {
  def this(s : String) = this(Some(Var.getNextVariableName(s)))
}
class SiteVariable(optionalName: Option[String] = None) extends Var(optionalName) {
  def this(s : String) = this(Some(Var.getNextVariableName(s)))
}
class ClosureVariable(optionalName: Option[String] = None) extends Var(optionalName) with PorcInfixClosureVariable {
  def this(s : String) = this(Some(Var.getNextVariableName(s)))
}


sealed abstract class Command extends PorcAST with FreeVariables with Substitution[Command]

case class Let(d: ClosureDef, k: Command) extends Command with hasSimpleContinuation
case class Site(defs: List[SiteDef], k: Command) extends Command with hasSimpleContinuation

case class SiteDef(name: SiteVariable, arguments: List[Variable], body: Command) extends PorcAST
case class ClosureDef(name: ClosureVariable, arguments: List[Variable], body: Command) extends PorcAST

case class ClosureCall(target: Value, argument: List[Value]) extends Command
case class SiteCall(target: Value, argument: List[Value]) extends Command

case class Unpack(variables: List[Variable], v: Value, k: Command) extends Command with hasSimpleContinuation

// ==================== PROCESS ===================

case class Spawn(target: ClosureVariable, k: Command) extends Command with hasSimpleContinuation
case class Die() extends Command

case class NewCounter(k: Command) extends Command with hasSimpleContinuation
case class RestoreCounter(zeroBranch: Command, nonzeroBranch: Command) extends Command
case class SetCounterHalt(haltCont: ClosureVariable, k: Command) extends Command with hasSimpleContinuation
case class GetCounterHalt(x: ClosureVariable, k: Command) extends Command with hasSimpleContinuation
case class DecrCounter(k: Command) extends Command with hasSimpleContinuation

case class NewTerminator(k: Command) extends Command with hasSimpleContinuation
case class GetTerminator(x: Variable, k: Command) extends Command with hasSimpleContinuation
case class Kill(killedBranch: Command, alreadykilledBranch: Command) extends Command
case class IsKilled(killedBranch: Command, notKilledBranch: Command) extends Command
case class AddKillHandler(u: Value, m: ClosureVariable, k: Command) extends Command with hasSimpleContinuation
case class CallKillHandlers(k: Command) extends Command with hasSimpleContinuation

// ==================== FUTURE ===================

case class NewFuture(x: Variable, k: Command) extends Command with hasSimpleContinuation
case class Force(futures: Value, boundBranch: Value, haltedBranch: Value) extends Command
case class Bind(future: Variable, value: Value, k: Command) extends Command with hasSimpleContinuation
case class Stop(future: Variable, k: Command) extends Command with hasSimpleContinuation

// ==================== FLAG ===================

case class NewFlag(x: Variable, k: Command) extends Command with hasSimpleContinuation
case class SetFlag(flag: Variable, k: Command) extends Command with hasSimpleContinuation
case class ReadFlag(flag: Variable, trueBranch: Command, falseBranch: Command) extends Command

// ==================== EXT ====================

case class ExternalCall(site: sites.Site, arguments: Value, p: Value, h: Value) extends Command

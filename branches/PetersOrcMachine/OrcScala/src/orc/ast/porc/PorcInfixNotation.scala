//
// PorcInfixNotation.scala -- Scala class/trait/object PorcInfixNotation
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

/**
  *
  * @author amp
  */
trait PorcInfixClosureVariable {
  this: ClosureVariable =>
  def apply(args: Variable*) = new {
    def ===(body: Command) = ClosureDef(PorcInfixClosureVariable.this, args.toList, body)
  }
}

trait PorcInfixValue {
  this: Value =>
  def apply(arg: Tuple) = ClosureCall(this, arg)
  def sitecall(arg: Tuple) = SiteCall(this, arg)
}

trait PorcInfixBinder[VT <: Variable, T <: Command] {
  def varConstructor(): VT
  val commandConstructor: (VT) => (Command) => T
  
  def apply(f: (VT) => Command) : T = {
    val x = varConstructor()
    commandConstructor(x)(f(x))
  }
}

object PorcInfixNotation { 
  implicit val reflectiveCalls = scala.language.reflectiveCalls
  
  /*implicit class SequentialCommand(val func: (Command) => Command) extends AnyVal {
    def >>(c: Command) = func(c)
  }*/
  
  def let(defs: ClosureDef*)(body: Command): Let = Let(defs.toList, body)
  
  def spawn(t: ClosureVariable)(k: Command): Spawn = Spawn(t, k)
  def restoreCounter(a: Command)(b: Command): RestoreCounter = RestoreCounter(a, b)
  def setCounterHalt(t: ClosureVariable)(k: Command): SetCounterHalt = SetCounterHalt(t, k)
  def getCounterHalt(f: (ClosureVariable) => Command): GetCounterHalt = {
    val x = new ClosureVariable()
    GetCounterHalt(x, f(x))
  }

  def getTerminator(f: (Variable) => Command): GetTerminator = {
    val x = new Variable("term")
    GetTerminator(x, f(x))
  }

  def kill(a: Command)(b: Command): Kill = Kill(a, b)

  def addKillHandler(t: Variable, v: ClosureVariable)(k: Command): AddKillHandler = AddKillHandler(t, v, k)

  // ==================== FUTURE ===================

  def future(f: (Variable) => Command): NewFuture = {
    val x = new Variable("future")
    NewFuture(x, f(x))
  }
  def future(x: Variable)(k: Command): NewFuture = NewFuture(x, k)

  def bind(t: Variable, v: Value)(k: Command): Bind = Bind(t, v, k)
  def stop(t: Variable)(k: Command): Stop = Stop(t, k)

  // ==================== FLAG ===================

  def flag(f: (Variable) => Command): NewFlag = {
    val x = new Variable("flag")
    NewFlag(x, f(x))
  }
  def flag(x: Variable)(k: Command): NewFlag = NewFlag(x, k)
  def setFlag(t: Variable)(k: Command): SetFlag = SetFlag(t, k)

  def readFlag(f: Variable)(a: Command)(b: Command): ReadFlag = ReadFlag(f, a, b)
}
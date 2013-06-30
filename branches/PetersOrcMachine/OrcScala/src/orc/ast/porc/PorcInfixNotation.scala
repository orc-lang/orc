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
  * @author amp
  */
trait PorcInfixClosureVariable {
  this: ClosureVariable =>
  def apply(args: Variable*) = new {
    def ===(body: Command) = ClosureDef(PorcInfixClosureVariable.this, args.toList, body)
  }
}

trait WithContextInfixCombinator {
  self: PorcAST =>
  def in(ctx: TransformContext): WithContext[this.type] = WithContext(this, ctx)
}

object in {
  def unapply[E <: PorcAST](c: WithContext[E]): Option[(E, TransformContext)] = Some((c.e, c.ctx))
}

trait PorcInfixValue {
  this: Value =>
  def apply(arg: Value*) = ClosureCall(this, arg.toList)
  def sitecall(arg: Value*) = SiteCall(this, arg.toList)
}

trait PorcInfixBinder[VT <: Variable, T <: Command] {
  def varConstructor(): VT
  val commandConstructor: (VT) => (Command) => T

  def apply(f: (VT) => Command): T = {
    val x = varConstructor()
    commandConstructor(x)(f(x))
  }
}

object PorcInfixNotation {
  implicit val reflectiveCalls = scala.language.reflectiveCalls

  /*implicit class SequentialCommand(val func: (Command) => Command) extends AnyVal {
    def >>(c: Command) = func(c)
  }*/

  def let(defs: ClosureDef*)(body: Command): Command = if (defs.isEmpty) body else Let(defs.head, let(defs.tail: _*)(body))

  def spawn(t: ClosureVariable)(k: Command): Spawn = Spawn(t, k)
  def restoreCounter(a: Command)(b: Command): RestoreCounter = RestoreCounter(a, b)
  def setCounterHalt(t: ClosureVariable)(k: Command): SetCounterHalt = SetCounterHalt(t, k)
  def getCounterHalt(f: (ClosureVariable) => Command): GetCounterHalt = {
    val x = new ClosureVariable("ch")
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

object LetIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (l@Let(d, b)) in ctx =>
      val bodyctx = ctx + LetBound(ctx, l)
      Some(d in ctx, b in bodyctx)
    case _ => None
  }
}

object SiteIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@Site(ds, b)) in ctx =>
      val bodyctx = ctx extendBindings ds.map(SiteBound(ctx, s, _))
      val sitectx = ctx extendBindings ds.map(RecursiveSiteBound(ctx, s, _))
      Some(ds, sitectx, b in bodyctx)
    case _ => None
  }
}


object SiteDefIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@SiteDef(n, args, b)) in ctx =>
      val bodyctx = ctx setCounterTerminator(s) extendBindings args.map(SiteArgumentBound(ctx, s, _))
      Some(n, args, bodyctx, b in bodyctx)
    case _ => None
  }
}

object ClosureDefIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@ClosureDef(n, args, b)) in ctx =>
      val bodyctx = ctx extendBindings args.map(LetArgumentBound(ctx, s, _))
      Some(n, args, bodyctx, b in bodyctx)
    case _ => None
  }
}


object ClosureCallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c@ClosureCall(t, args)) in ctx => Some(t in ctx, args, ctx)
    case _ => None
  }
}

object SiteCallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c@SiteCall(t, args)) in ctx => Some(t in ctx, args, ctx)
    case _ => None
  }
}

object UnpackIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@Unpack(vs, v, k)) in ctx =>
      val bodyctx = ctx extendBindings vs.map(UnpackBound(ctx, s, _))
      Some(vs, v, k in bodyctx)
    case _ => None
  }
}


// ==================== PROCESS ===================

object SpawnIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c@Spawn(t, k)) in ctx => Some(t in ctx, k in ctx)
    case _ => None
  }
}

object NewCounterIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@NewCounter(k)) in ctx => Some(k in ctx.setCounter(n))
    case _ => None
  }
}
object DecrCounterIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@DecrCounter(k)) in ctx => Some(k in ctx)
    case _ => None
  }
}

object RestoreCounterIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@RestoreCounter(ze, no)) in ctx =>
      val bodyctx = ctx.setCounter(n)
      Some(ze in bodyctx, no in bodyctx)
    case _ => None
  }
}

object SetCounterHaltIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@SetCounterHalt(v, k)) in ctx => Some(v, k in ctx)
    case _ => None
  }
}

object GetCounterHaltIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@GetCounterHalt(x, k)) in ctx =>
      val bodyctx = ctx + StrictBound(ctx, n, x)
      Some(x, k in bodyctx)
    case _ => None
  }
}


object NewTerminatorIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@NewTerminator(k)) in ctx =>
      val bodyctx = ctx.setTerminator(n)
      Some(k in bodyctx)
    case _ => None
  }
}

object GetTerminatorIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@GetTerminator(x, k)) in ctx =>
      val bodyctx = ctx + StrictBound(ctx, n, x)
      Some(x, k in bodyctx)
    case _ => None
  }
}

object KillIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@Kill(k, a)) in ctx => Some(k in ctx, a in ctx)
    case _ => None
  }
}

object IsKilledIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@IsKilled(k, no)) in ctx => Some(k in ctx, no in ctx)
    case _ => None
  }
}

object AddKillHandlerIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@AddKillHandler(u, m, k)) in ctx => Some(u in ctx, m in ctx, k in ctx)
    case _ => None
  }
}

object CallKillHandlersIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case CallKillHandlers(k) in ctx => Some(k in ctx)
    case _ => None
  }
}


// ==================== FUTURE ===================

object NewFutureIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@NewFuture(x, k)) in ctx => Some(x, k in ctx + StrictBound(ctx, n, x))
    case _ => None
  }
}

object ForceIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Force(f, b, h) in ctx => Some(f in ctx, b in ctx, h in ctx)
    case _ => None
  }
}

object BindIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Bind(f, v, k) in ctx => Some(f in ctx, v in ctx, k in ctx)
    case _ => None
  }
}

object StopIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Stop(f, k) in ctx => Some(f in ctx, k in ctx)
    case _ => None
  }
}


// ==================== FLAG ===================

object NewFlagIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@NewFlag(x, k)) in ctx => Some(x in ctx, k in ctx + StrictBound(ctx, n, x))
    case _ => None
  }
}

object SetFlagIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case SetFlag(flag, k) in ctx => Some(flag in ctx, k in ctx)
    case _ => None
  }
}

object ReadFlagIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@ReadFlag(flag, t, f)) in ctx => Some(flag in ctx, t in ctx, f in ctx)
    case _ => None
  }
}


// ==================== EXT ====================

object ExternalCallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case ExternalCall(site, args, p, h) in ctx => Some(site, args in ctx, p in ctx, h in ctx)
    case _ => None
  }
}


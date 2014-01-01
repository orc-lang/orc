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

trait WithContextInfixCombinator {
  self: PorcAST =>
  def in(ctx: TransformContext): WithContext[this.type] = WithContext(this, ctx)
}

object in {
  def unapply[E <: PorcAST](c: WithContext[E]): Option[(E, TransformContext)] = Some((c.e, c.ctx))
}

trait PorcInfixValue {
  this: Value =>
  def apply(arg: Value*) = Call(this, arg.toList)
  def sitecall(p: Value, arg: Value*) = SiteCall(this, arg.toList, p)
}
trait PorcInfixExpr {
  this: Expr =>
  def :::(f: Expr): Sequence = Sequence(f, this)
}

object PorcInfixNotation {
  implicit val reflectiveCalls = scala.language.reflectiveCalls

  def let(defs: (Var, Expr)*)(body: Expr): Expr = if (defs.isEmpty) body else Let(defs.head._1, defs.head._2, let(defs.tail: _*)(body))
  def lambda(args: Var*)(body: Expr): Expr = Lambda(args.toList, body)

  def restoreCounter(a: Expr)(b: Expr): RestoreCounter = RestoreCounter(a, b)
}

object LetIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (l@Let(x, v, b)) in ctx =>
      val bodyctx = ctx + LetBound(ctx, l)
      Some(x in ctx, v in ctx, b in bodyctx)
    case _ => None
  }
}
object LambdaIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (l@Lambda(args, b)) in ctx =>
      val bodyctx = ctx extendBindings args.map(LambdaArgumentBound(ctx, l, _))
      Some(args, bodyctx, b in bodyctx)
    case _ => None
  }
}

object SequenceIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Sequence(l) in ctx =>
      Some(l, ctx)
    case _ => None
  }
}

object SiteIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@Site(ds, b)) in ctx =>
      val bodyctx = ctx extendBindings ds.map(SiteBound(ctx, s, _))
      val sitectx = ctx extendBindings (ds.map(RecursiveSiteBound(ctx, s, _)))
      Some(ds, sitectx, b in bodyctx)
    case _ => None
  }
}


object SiteDefIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@SiteDef(n, args, p, b)) in ctx =>
      val bodyctx = ctx setCounterTerminator(s) extendBindings args.map(SiteArgumentBound(ctx, s, _)) extendBindings List(SitePublishBound(ctx, s))
      Some(n, args, p, bodyctx, b in bodyctx)
    case _ => None
  }
}

object CallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c@Call(t, args)) in ctx => Some(t in ctx, args, ctx)
    case _ => None
  }
}

object SiteCallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c@SiteCall(t, args, p)) in ctx => Some(t in ctx, args, p, ctx)
    case _ => None
  }
}
object DirectSiteCallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c@DirectSiteCall(t, args)) in ctx => Some(t in ctx, args, ctx)
    case _ => None
  }
}

object TryOnHaltedIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case TryOnHalted(b, h) in ctx => Some(b in ctx, h in ctx)
    case _ => None
  } 
}
object TryOnKilledIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case TryOnKilled(b, h) in ctx => Some(b in ctx, h in ctx)
    case _ => None
  } 
}

/*object ProjectIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@Project(n, v)) in ctx =>
      Some(n, v in ctx)
    case _ => None
  }
}*/


// ==================== PROCESS ===================

object SpawnIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c@Spawn(t)) in ctx => Some(t in ctx)
    case _ => None
  }
}

object NewCounterIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@NewCounter(k)) in ctx => Some(k in ctx.setCounter(n))
    case _ => None
  }
}

/*object NewCounterDisconnectedIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@NewCounterDisconnected(k)) in ctx => Some(k in ctx.setCounter(n))
    case _ => None
  }
}*/

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
    case (n@SetCounterHalt(v)) in ctx => Some(v in ctx)
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

object AddKillHandlerIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@AddKillHandler(u, m)) in ctx => Some(u in ctx, m in ctx)
    case _ => None
  }
}
object IsKilledIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@IsKilled(t)) in ctx => Some(t in ctx)
    case _ => None
  }
}

object KillIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@Kill(ze, no)) in ctx =>
      val bodyctx = ctx.setTerminator(n)
      Some(ze in bodyctx, no in bodyctx)
    case _ => None
  }
}


// ==================== FUTURE ===================

object ForceIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Force(f, b) in ctx => Some(f, ctx, b in ctx)
    case _ => None
  }
}

object ResolveIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Resolve(f, b) in ctx => Some(f in ctx, b in ctx)
    case _ => None
  }
}

object BindIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Bind(f, v) in ctx => Some(f in ctx, v in ctx)
    case _ => None
  }
}

object StopIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Stop(f) in ctx => Some(f in ctx)
    case _ => None
  }
}


// ==================== FLAG ===================

object SetFlagIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case SetFlag(flag) in ctx => Some(flag in ctx)
    case _ => None
  }
}

object ReadFlagIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (n@ReadFlag(flag)) in ctx => Some(flag in ctx)
    case _ => None
  }
}


// ==================== EXT ====================

object ExternalCallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case ExternalCall(site, args, p) in ctx => Some(site, args, ctx, p in ctx)
    case _ => None
  }
}


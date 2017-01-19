//
// Analysis.scala -- Scala class/trait/object Analysis
// Project OrcScala
//
// Created by amp on Jun 2, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import scala.collection.mutable
import orc.values.Field
import orc.values.sites.{ Site => OrcSite }
import orc.values.sites.SiteMetadata
import orc.values.sites.Delay

case class AnalysisResults(
  isNotFuture: Boolean,
  doesNotThrowHalt: Boolean,
  fastTerminating: Boolean,
  siteMetadata: Option[SiteMetadata]) {
}

sealed trait AnalysisProvider[E <: PorcAST] {
  outer =>
  def apply(e: WithContext[E]): AnalysisResults
  def get(e: WithContext[E]): Option[AnalysisResults]

  object ImplicitResults {
    import scala.language.implicitConversions
    implicit def expressionCtxWithResults(e: WithContext[E]): AnalysisResults = apply(e)
  }

  def withDefault: AnalysisProvider[E] = {
    new AnalysisProvider[E] {
      def apply(e: WithContext[E]): AnalysisResults = get(e).getOrElse(AnalysisResults(false, false, false, None))
      def get(e: WithContext[E]): Option[AnalysisResults] = outer.get(e)
    }
  }
}

/** A cache for storing all the results of a bunch of expressions.
  */
class Analyzer extends AnalysisProvider[PorcAST] {
  val cache = mutable.Map[WithContext[PorcAST], AnalysisResults]()

  // TODO: Somehow this is running the system out of memory on some programs. For example, /OrcExamples/OrcSites/simanim/baboon.orc

  def apply(e: WithContext[PorcAST]) = {
    cache.get(e) match {
      case Some(r) => {
        r
      }
      case None => {
        val r = analyze(e)
        cache += e -> r
        r
      }
    }
  }
  def get(e: WithContext[PorcAST]) = Some(apply(e))

  def analyze(e: WithContext[PorcAST]): AnalysisResults = {
    AnalysisResults(nonFuture(e), nonHalt(e), fastTerminating(e), siteMetadata(e))
  }

  def translateArguments(vs: List[Value], formals: List[Var], s: Set[Var]): Set[Var] = {
    val m = (formals zip vs).toMap
    s.collect(m).collect { case v: Var => v }
  }

  def nonFuture(e: WithContext[PorcAST]): Boolean = {
    import ImplicitResults._
    e match {
      case (_: OrcValue | _: Unit) in _ => true
      case (_: SiteCallDirect) in _ => true
      case (_: DefCallDirect) in _ => true
      case (v: Var) in ctx => ctx(v) match {
        // Somehow hack in enough context dependance to catch obvious cases of arguments that have nonfuture args.
        case ContinuationArgumentBound(ctx, cont, _) =>
          // Continuation args can never be a future or an unresolved closure because they are publication values
          true
        case LetBound(lctx, l) => {
          val LetIn(x, v, _) = l in lctx
          v.isNotFuture
        }
        case _ => false // This needs types.
      }
      case _ => false
    }
  }

  def siteMetadata(e: WithContext[PorcAST]): Option[SiteMetadata] = {
    import ImplicitResults._
    e match {
      case OrcValue(s: SiteMetadata) in _ => Some(s)
      case (x: Var) in ctx => ctx(x) match {
        case LetBound(ctx2, l: Let) =>
          val LetIn(_, v, b) = l in ctx2
          v.siteMetadata
        case _ => None
      }
      case SiteCallIn(target, p, c, t, args, ctx) =>
        target.siteMetadata flatMap { sm =>
          sm.returnMetadata(args.map {
            _ match {
              case OrcValue(v) => Some(v)
              case _ => None
            }
          })
        }
      case SiteCallDirectIn(target, args, ctx) =>
        target.siteMetadata flatMap { sm =>
          sm.returnMetadata(args.map {
            _ match {
              case OrcValue(v) => Some(v)
              case _ => None
            }
          })
        }
      case _ => None
    }
  }

  def nonHalt(e: WithContext[PorcAST]): Boolean = {
    import ImplicitResults._
    // TODO: Fill this out to be a more accurate analysis. It should be a type analysis.
    e match {
      case (_: OrcValue | _: Unit | _: Var) in _ => true
      case ContinuationIn(_, _, _) => true
      case SiteCallIn(_, _, _, _, _, _) => true
      case SiteCallDirectIn(target, _, _) => target.siteMetadata.map(_.publications > 0).getOrElse(false)
      case DefCallIn(_, _, _, _, _, _) => true
      case DefCallDirectIn(target, _, _) => false

      /*
      case CallIn((t: Var) in ctx, _, _) => ctx(t) match {
        case LetBound(dctx, l) => 
          val LetIn(_, LambdaIn(_, _, b), _) = l in dctx
          b.doesNotThrowHalt
        case _ => false
      }
      
      case IfIn(p, t, e) => t.doesNotThrowHalt && e.doesNotThrowHalt
      case TryOnHaltedIn(b, h) => h.doesNotThrowHalt
      case TryOnKilledIn(b, h) => b.doesNotThrowHalt && h.doesNotThrowHalt
      case SequenceIn(l, ctx) => l forall (e => (e in ctx).doesNotThrowHalt)
      case LetIn(_, v, b) => v.doesNotThrowHalt && b.doesNotThrowHalt
      case KillIn(b, a) => b.doesNotThrowHalt && a.doesNotThrowHalt
      case RestoreCounterIn(b, a) => b.doesNotThrowHalt && a.doesNotThrowHalt
      case NewCounterIn(b) => b.doesNotThrowHalt
      case NewTerminatorIn(b) => b.doesNotThrowHalt
      case (NewFlag() | NewFuture()) in _ => true
      */
      case _ => false
    }
  }

  // TODO: detect more fast cases for defcall and call. This is important for eliminating spawns at def calls when they are not needed.
  def fastTerminating(e: WithContext[PorcAST]): Boolean = {
    import ImplicitResults._
    e match {
      case SiteCallDirectIn(target, args, _) if target.siteMetadata.map(_.timeToHalt == Delay.NonBlocking).getOrElse(false) => {
        true
      }
      case SiteCallIn(_, p, c, t, args, _) => true

      case DefCallIn((target: Var) in ctx, p, c, t, args, _) => {
        ctx(target) match {
          case DefBound(_, _, _) => true
          case _ => false
        }
      }

      case DefCallIn(target, p, c, t, args, _) => false

      case CallIn((t: Var) in ctx, _, _) => {
        def resolveLet(e: WithContext[PorcAST]): WithContext[PorcAST] = e match {
          case (t: Var) in ctx => ctx(t) match {
            case LetBound(dctx, l) =>
              resolveLet(l.v in dctx)
            case _ => e
          }
          case _ => e
        }
        ctx(t) match {
          case LetBound(dctx, l) =>
            resolveLet(l.v in dctx) match {
              case ContinuationIn(_, _, b) =>
                b.fastTerminating
              case _ => false
            }
          case SpawnFutureBound(_, _, _) =>
            true
          case DefArgumentBound(_, _, _) =>
            false
          case _ => false
        }
      }

      case TryOnHaltedIn(b, h) => b.fastTerminating && h.fastTerminating
      case TryOnKilledIn(b, h) => b.fastTerminating && h.fastTerminating
      case LetIn(_, v, b) => v.fastTerminating && b.fastTerminating
      case SequenceIn(l, ctx) => l forall (e => (e in ctx).fastTerminating)
      case NewCounterIn(_, b) => b.fastTerminating
      case NewTerminatorIn(b) => b.fastTerminating
      case (_: Continuation | _: Spawn | _: SpawnFuture | _: Force | _: Unit | _: Kill) in _ => {
        true
      }
      case _ => false
    }
  }
}

object Analysis {
  val closureCost = 5
  val spawnCost = 4
  val forceCost = 3
  val killCost = 2
  val callkillhandlersCost = 5
  val callCost = 1
  val externalCallCost = 5
  val atomicOperation = 2

  def cost(e: PorcAST): Int = {
    val cs = e.subtrees.asInstanceOf[Iterable[PorcAST]]
    (e match {
      case _: Spawn => spawnCost
      case _: Force => forceCost
      case _: Kill => killCost
      case _: Continuation | _: Def | _: NewCounter => closureCost
      case _: NewTerminator => closureCost
      case _: Call => callCost
      case _: SiteCallDirect => externalCallCost
      case _: SiteCall => externalCallCost + spawnCost
      case _: Kill | _: Halt => atomicOperation
      case _ => 0
    }) + (cs.map(cost).sum)
  }

  def count(t: PorcAST, p: (Expr => Boolean)): Int = {
    val cs = t.subtrees.asInstanceOf[Iterable[PorcAST]]
    (t match {
      case e: Expr if p(e) => 1
      case _ => 0
    }) +
      (cs.map(count(_, p)).sum)
  }
}

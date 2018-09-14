//
// Rclock.scala -- Scala objects Rwait and Rclock, and class Rtime
// Project OrcScala
//
// Created by dkitchin on Jan 13, 2011.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import scala.math.BigInt.int2bigInt

import orc.{ VirtualCallContext, OrcRuntime, DirectInvoker }
import orc.run.extensions.RwaitEvent
import orc.types.{ IntegerType, RecordType, SignalType, SimpleFunctionType }
import orc.values.{ Field, OrcRecord, Signal, HasMembersMetadata, ValueMetadata }
import orc.values.sites.{ EffectFreeSite, FunctionalSite, Site1Base, SiteMetadata, TalkativeSite, TotalSite0Base, TypedSite }

/**
  */
object Rclock extends TotalSite0Base with TypedSite with FunctionalSite with TalkativeSite {
  def getInvoker(runtime: OrcRuntime): DirectInvoker = {
    invoker(this) { _ =>
      new OrcRecord(
        "time" -> new Rtime(System.currentTimeMillis()),
        "wait" -> Rwait)
    }
  }

  def orcType =
    SimpleFunctionType(
      new RecordType(
        "time" -> SimpleFunctionType(IntegerType),
        "wait" -> SimpleFunctionType(IntegerType, SignalType)))

  override def publicationMetadata(args: List[Option[AnyRef]]): Option[HasMembersMetadata] = Some(new HasMembersMetadata {
    override def fieldMetadata(f: Field): Option[SiteMetadata] = f match {
      case Field("time") => Some(new Rtime(0))
      case Field("wait") => Some(Rwait)
      case _ => None
    }
  })
}

/**
  */
class Rtime(val startTime: Long) extends TotalSite0Base with FunctionalSite {
  def getInvoker(runtime: OrcRuntime): DirectInvoker = {
    invoker(this) { self =>
      System.currentTimeMillis() - self.startTime
    }
  }
}

/**
  */
object Rwait extends Site1Base[Number] with EffectFreeSite {
  final def doit(callContext: VirtualCallContext, delay: BigInt) = {
    if (delay > 0) {
      val ctx = callContext.materialize()
      ctx.setQuiescent()
      callContext.notifyOrc(RwaitEvent(delay, ctx))
      callContext.empty()
    } else if (delay == 0) {
      callContext.publish(Signal)
    } else {
      callContext.halt()
    }
  }

  def getInvoker(runtime: OrcRuntime, a: Number) = {
    a match {
      case delay: BigInt =>
        invoker(this, delay)((ctx, _, a) => doit(ctx, a))
      case delay: java.lang.Long =>
        invoker(this, delay)((ctx, _, a) => doit(ctx, BigInt(a)))
      case delay: java.lang.Number =>
        invoker(this, delay)((ctx, _, a) => doit(ctx, BigInt(a.longValue())))
    }
  }
}

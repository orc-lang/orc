//
// Rclock.scala -- Scala objects Rwait and Rclock, and class Rtime
// Project OrcScala
//
// Created by dkitchin on Jan 13, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import scala.math.BigInt.int2bigInt

import orc.CallContext
import orc.error.runtime.ArgumentTypeMismatchException
import orc.run.extensions.RwaitEvent
import orc.types.{ IntegerType, RecordType, SignalType, SimpleFunctionType }
import orc.values.{ Field, OrcRecord, Signal }
import orc.values.sites.{ EffectFreeSite, FunctionalSite, Site1, SiteMetadata, TalkativeSite, TotalSite0, TypedSite }

/**
  */
object Rclock extends TotalSite0 with TypedSite with FunctionalSite with TalkativeSite {

  def eval() = {
    new OrcRecord(
      "time" -> new Rtime(System.currentTimeMillis()),
      "wait" -> Rwait)
  }

  def orcType =
    SimpleFunctionType(
      new RecordType(
        "time" -> SimpleFunctionType(IntegerType),
        "wait" -> SimpleFunctionType(IntegerType, SignalType)))

  override def returnMetadata(args: List[Option[AnyRef]]): Option[SiteMetadata] = Some(new SiteMetadata {
    override def fieldMetadata(f: Field): Option[SiteMetadata] = f match {
      case Field("time") => Some(new Rtime(0))
      case Field("wait") => Some(Rwait)
      case _ => None
    }
  })
}

/**
  */
class Rtime(startTime: Long) extends TotalSite0 with FunctionalSite {

  def eval() = (System.currentTimeMillis() - startTime).asInstanceOf[AnyRef]

}

/**
  */
object Rwait extends Site1 with EffectFreeSite {

  def call(a: AnyRef, callContext: CallContext) {
    a match {
      case delay: BigInt => {
        if (delay > 0) {
          callContext.setQuiescent()
          callContext.notifyOrc(RwaitEvent(delay, callContext))
        } else if (delay == 0) {
          callContext.publish(Signal)
        } else {
          callContext.halt
        }
      }
      case _ => throw new ArgumentTypeMismatchException(0, "Integer", if (a != null) a.getClass().toString() else "null")
    }
  }

}

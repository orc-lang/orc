//
// Rwait.scala -- Scala objects Rwait and Rclock, and class Rtime
// Project OrcScala
//
// Created by dkitchin on Jan 13, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import scala.math.BigInt.int2bigInt
import orc.Handle
import orc.error.runtime.ArgumentTypeMismatchException
import orc.run.extensions.RwaitEvent
import orc.types.{ SimpleFunctionType, SignalType, RecordType, IntegerType }
import orc.values.sites.{ TypedSite, TotalSite0, Site1 }
import orc.values.OrcRecord
import orc.values.sites.FunctionalSite
import orc.values.sites.EffectFreeSite

/**
  */
object Rclock extends TotalSite0 with TypedSite with FunctionalSite {

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

}

/**
  */
class Rtime(startTime: Long) extends TotalSite0 with FunctionalSite {

  def eval() = (System.currentTimeMillis() - startTime).asInstanceOf[AnyRef]

}

/**
  */
object Rwait extends Site1 with EffectFreeSite {

  def call(a: AnyRef, h: Handle) {
    a match {
      case delay: BigInt => {
        if (delay > 0) {
          h.setQuiescent()
          h.notifyOrc(RwaitEvent(delay, h))
        } else if (delay == 0) {
          h.publish()
        } else {
          h.halt
        }
      }
      case _ => throw new ArgumentTypeMismatchException(0, "Integer", if (a != null) a.getClass().toString() else "null")
    }
  }

}

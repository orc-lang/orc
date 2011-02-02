//
// Rtimer.scala -- Scala class/trait/object Rtimer
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 13, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import orc.values.sites.Site
import orc.values.sites.TypedSite
import orc.values.sites.TotalSite0
import orc.types.SimpleFunctionType
import orc.types.NumberType
import orc.types.IntegerType
import orc.types.SignalType
import orc.types.FieldType
import orc.types.OverloadedType
import orc.run.extensions.RtimerEvent
import orc.Handle
import orc.values.Field
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

/**
 * 
 *
 * @author dkitchin
 */
class Rtimer extends Site with TypedSite {

  def call(args: List[AnyRef], caller: Handle) {
    args match {
      case List(delay: BigInt) => {
        caller.notifyOrc(RtimerEvent(delay, caller))
      }
      case List(Field("time")) => Rclock
      case List(a) => throw new ArgumentTypeMismatchException(0, "Integer", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }
  
  }
  
  def orcType = OverloadedType(List(
    SimpleFunctionType(IntegerType, SignalType),
    SimpleFunctionType(FieldType("time"), Rclock.orcType)
  ))
  
}

object Rclock extends TotalSite0 with TypedSite {
  def eval() = System.currentTimeMillis().asInstanceOf[AnyRef]
  def orcType = SimpleFunctionType(NumberType)
}

//
// Rwait.scala -- Scala objects Rwait and Rclock, and class Rtime
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

import orc.values.sites.Site1
import orc.values.sites.TypedSite
import orc.values.sites.TotalSite0
import orc.values.OrcRecord
import orc.types.SimpleFunctionType
import orc.types.RecordType
import orc.types.IntegerType
import orc.types.SignalType
import orc.types.OverloadedType
import orc.run.extensions.RwaitEvent
import orc.Handle
import orc.error.runtime.ArgumentTypeMismatchException


object Rclock extends TotalSite0 with TypedSite {
  
  def eval() = {
    new OrcRecord(
      "time" -> new Rtime(System.currentTimeMillis()),
      "wait" -> Rwait  
    )
  }
    
  def orcType = 
    SimpleFunctionType(
      new RecordType(
        "time" -> SimpleFunctionType(IntegerType),
        "wait" -> SimpleFunctionType(IntegerType, SignalType)
      )
    )
    
}

class Rtime(startTime: Long) extends TotalSite0 {
  
  def eval() = (System.currentTimeMillis() - startTime).asInstanceOf[AnyRef]
  
}

object Rwait extends Site1 {

  def call(a: AnyRef, h: Handle) {
    a match {
      case delay: BigInt => {
        if (delay > 0) { 
          h.notifyOrc(RwaitEvent(delay, h)) 
        } 
        else if (delay == 0) {
          h.publish() 
        }
        else {
          h.halt
        }
      }
      case _ => throw new ArgumentTypeMismatchException(0, "Integer", if (a != null) a.getClass().toString() else "null")
    }
  }
  
}



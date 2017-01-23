//
// Vclock.scala -- Scala object Vclock
// Project OrcScala
//
// Created by dkitchin on Aug 8, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import orc.error.runtime.RuntimeSupportException
import orc.run.core.VirtualClockOperation
import orc.Handle
import orc.types.{ FunctionType, SignalType, StrictCallableType, Top }
import orc.values.sites.{ Site1, TypedSite }

/** @author dkitchin
  */
object Vclock extends Site1 with VirtualClockOperation with TypedSite {

  // Do not invoke directly.
  def call(a: AnyRef, h: Handle) { h !! (new RuntimeSupportException("Vclock")) }

  lazy val orcType = {
    //val A = new TypeVariable()
    //val comparatorType = new FunctionType(List(A), List(A, A), IntegerType) with StrictType
    //new FunctionType(Nil, List(comparatorType), SignalType) with StrictType
    /*TODO: This typing is a placeholder */
    new FunctionType(Nil, List(Top), SignalType) with StrictCallableType
  }

}

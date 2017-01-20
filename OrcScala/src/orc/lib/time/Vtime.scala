//
// Vtime.scala -- Scala object Vtime
// Project OrcScala
//
// Created by dkitchin on Aug 8, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import orc.Handle
import orc.error.runtime.RuntimeSupportException
import orc.run.core.VirtualClockOperation
import orc.types.{ FunctionType, StrictCallableType, Top }
import orc.values.sites.{ Site0, TypedSite }

/** @author dkitchin
  */
object Vtime extends Site0 with VirtualClockOperation with TypedSite {

  // Do not invoke directly.
  def call(h: Handle) { h !! (new RuntimeSupportException("Vtime")) }

  lazy val orcType = {
    new FunctionType(Nil, Nil, Top) with StrictCallableType
  }

}

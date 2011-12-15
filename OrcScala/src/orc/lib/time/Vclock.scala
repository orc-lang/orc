//
// Vclock.scala -- Scala object Vclock
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 8, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import orc.error.runtime.RuntimeSupportException
import orc.run.core.VirtualClockOperation
import orc.values.sites.Site1

import orc.Handle

/** @author dkitchin
  */
object Vclock extends Site1 with VirtualClockOperation {
  // Do not invoke directly.
  def call(a: AnyRef, h: Handle) { h !! (new RuntimeSupportException("Vclock")) }
  override val quiescentWhileInvoked: Boolean = false
}

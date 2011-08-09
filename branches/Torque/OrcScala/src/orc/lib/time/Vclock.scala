//
// Vclock.scala -- Scala class/trait/object Vclock
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

import orc.Handle
import orc.values.sites.Site1
import orc.error.runtime.RuntimeSupportException

/**
 * 
 *
 * @author dkitchin
 */
object Vclock extends Site1 {
  // Do not invoke directly.
  def call(a: AnyRef, h: Handle) { h !! (new RuntimeSupportException("Vclock")) }
  override val quiescentWhileInvoked: Boolean = false
}
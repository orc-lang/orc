//
// Now.scala -- Scala class/trait/object Now
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
import orc.values.sites.Site0
import orc.error.runtime.RuntimeSupportException

/**
 * 
 *
 * @author dkitchin
 */
object Now extends Site0 {
  // Invocation behavior defined in orc.run.extensions.SupportForTimelines; do not invoke directly.
  def call(h: Handle) { h !! (new RuntimeSupportException("Now")) } 
  override val quiescentWhileInvoked: Boolean = false
}
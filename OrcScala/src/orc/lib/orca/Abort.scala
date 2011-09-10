//
// Abort.scala -- Scala class/trait/object Abort
// Project Orca
//
// $Id$
//
// Created by dkitchin on Jun 27, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.orca

import orc.values.sites.Site
import orc.Handle
import orc.orca.Transaction

/**
 * 
 * Abort the closest enclosing transaction.
 *
 * @author dkitchin
 */
object Abort extends Site {
  def call(args: List[AnyRef], h: Handle) {
    h.context.abort()
    h.halt
  }
}
//
// If.scala -- Scala object If
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.builtin

import orc.oil._
import orc.sites.PartialSite

/**
 * 
 *
 * @author dkitchin
 */
object If extends PartialSite {
  override def name = "If"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Literal({}))
      case _ => None
  }
}

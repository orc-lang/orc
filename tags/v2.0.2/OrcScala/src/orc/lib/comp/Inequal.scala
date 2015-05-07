//
// NotEq.scala -- Scala object Inequal
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 9, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.comp

import scala.Integer
import orc.values.sites.TotalSite
import orc.values.sites.UntypedSite
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.ArgumentTypeMismatchException

/**
 * Not equals comparison -- delegates to Scala's ==
 */
object Inequal extends TotalSite with UntypedSite {
  override def name = "Inequal"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(a, b) => new java.lang.Boolean(!(a == b))
      case _ => throw new ArityMismatchException(2, args.size)
    }
}

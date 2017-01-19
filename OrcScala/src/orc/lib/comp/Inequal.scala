//
// NotEq.scala -- Scala object Inequal
// Project OrcScala
//
// Created by jthywiss on Jun 9, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.comp

import orc.error.runtime.ArityMismatchException
import orc.values.sites._
import orc.util.ArrayExtensions._

/** Not equals comparison -- delegates to Scala's ==
  */
object Inequal extends TotalSite with UntypedSite {
  override def name = "Inequal"
  def evaluate(args: Array[AnyRef]) =
    args match {
      case Array2(a, b) => new java.lang.Boolean(!(a == b))
      case _ => throw new ArityMismatchException(2, args.size)
    }
}

//
// Not.scala -- Scala object Not
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.bool

import orc.values.Value
import orc.values.Literal
import orc.values.sites.TotalSite
import orc.values.sites.UntypedSite
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.ArgumentTypeMismatchException

/**
 * Logical negation site
 */
object Not extends TotalSite with UntypedSite {
  override def name = "Not"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Literal(false)
      case List(Literal(false)) => Literal(true)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

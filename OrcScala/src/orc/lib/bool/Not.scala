//
// Not.scala -- Scala object Not
// Project OrcScala
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.bool

import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException }
import orc.types.{ BooleanType, SimpleFunctionType }
import orc.values.sites.{ FunctionalSite, TotalSite, TypedSite }
import orc.util.ArrayExtensions._

/** Logical negation site
  */
object Not extends TotalSite with TypedSite with FunctionalSite {
  override def name = "Not"
  def evaluate(args: Array[AnyRef]) =
    args match {
      case Array1(b: java.lang.Boolean) => new java.lang.Boolean(!b.booleanValue)
      case Array1(a) => throw new ArgumentTypeMismatchException(0, "Boolean", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }

  def orcType() = SimpleFunctionType(BooleanType, BooleanType)
}

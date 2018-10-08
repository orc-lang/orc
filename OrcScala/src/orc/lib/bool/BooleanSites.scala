//
// Not.scala -- Scala object Not
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.bool

import orc.types.{ BooleanType, SimpleFunctionType }
import orc.values.sites.{ FunctionalSite, TalkativeSite, OverloadedDirectInvokerMethod1, OverloadedDirectInvokerMethod2 }

/** Logical negation site
  */
object Not extends OverloadedDirectInvokerMethod1[java.lang.Boolean] with FunctionalSite with TalkativeSite {
  override def name = "Not"

  def getInvokerSpecialized(a: java.lang.Boolean) = {
    invoker(a)(a => !a)
  }

  def orcType() = SimpleFunctionType(BooleanType, BooleanType)
}

object Or extends OverloadedDirectInvokerMethod2[java.lang.Boolean, java.lang.Boolean] with FunctionalSite with TalkativeSite {
  override def name = "Or"

  def getInvokerSpecialized(a: java.lang.Boolean, b: java.lang.Boolean) = {
    invoker(a, b)((a, b) => a || b)
  }
}

object And extends OverloadedDirectInvokerMethod2[java.lang.Boolean, java.lang.Boolean] with FunctionalSite with TalkativeSite {
  override def name = "And"

  def getInvokerSpecialized(a: java.lang.Boolean, b: java.lang.Boolean) = {
    invoker(a, b)((a, b) => a && b)
  }
}

//
// Not.scala -- Scala object Not
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.bool

import orc.Invoker
import orc.types.{ BooleanType, SimpleFunctionType }
import orc.values.sites.{ FunctionalSite, LocalSingletonSite, OverloadedDirectInvokerMethod1, OverloadedDirectInvokerMethod2, TalkativeSite }

/** Logical negation site
  */
object Not extends OverloadedDirectInvokerMethod1[java.lang.Boolean] with FunctionalSite with TalkativeSite with Serializable with LocalSingletonSite {
  override def name = "Not"
  
  def getInvokerSpecialized(a: java.lang.Boolean): Invoker = {
    invoker(a)(a => !a)
  }
  
  def orcType() = SimpleFunctionType(BooleanType, BooleanType)
}

object Or extends OverloadedDirectInvokerMethod2[java.lang.Boolean, java.lang.Boolean] with FunctionalSite with TalkativeSite with Serializable with LocalSingletonSite {
  override def name = "Or"
  
  def getInvokerSpecialized(a: java.lang.Boolean, b: java.lang.Boolean): Invoker = {
    invoker(a, b)((a, b) => a || b)
  }
}

object And extends OverloadedDirectInvokerMethod2[java.lang.Boolean, java.lang.Boolean] with FunctionalSite with TalkativeSite with Serializable with LocalSingletonSite {
  override def name = "And"
  
  def getInvokerSpecialized(a: java.lang.Boolean, b: java.lang.Boolean): Invoker = {
    invoker(a, b)((a, b) => a && b)
  }
}

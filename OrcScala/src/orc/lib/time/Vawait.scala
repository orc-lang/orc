//
// Vawait.scala -- Scala object Vawait
// Project OrcScala
//
// Created by dkitchin on Aug 8, 2011.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.time

import orc.error.runtime.RuntimeSupportException
import orc.run.core.VirtualClockOperation
import orc.types.{ BooleanType, FunctionType, StrictCallableType, TypeVariable }
import orc.values.sites.{ LocalSingletonSite, TypedSite }
import orc.values.sites.compatibility.{ CallContext, Site1 }

/** @author dkitchin
  */
object Vawait extends Site1 with VirtualClockOperation with TypedSite with Serializable with LocalSingletonSite {

  // Do not invoke directly.
  def call(a: AnyRef, callContext: CallContext) { callContext !! (new RuntimeSupportException("Vawait")) }

  lazy val orcType = {
    val A = new TypeVariable()
    new FunctionType(List(A), List(A), BooleanType) with StrictCallableType
  }

}

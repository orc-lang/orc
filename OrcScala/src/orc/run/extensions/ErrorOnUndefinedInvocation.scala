//
// ErrorOnUndefinedInvocation.scala -- Scala trait ErrorOnUndefinedInvocation
// Project OrcScala
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.{ Accessor, ClassDoesNotHaveMembersAccessor, InvocationBehavior, Invoker, UncallableValueInvoker }
import orc.values.Field

/** @author dkitchin
  */
trait ErrorOnUndefinedInvocation extends InvocationBehavior {
  def getInvoker(target: AnyRef, arguments: Array[AnyRef]): Invoker = {
    UncallableValueInvoker(target)
  }
  
  def getAccessor(target: AnyRef, field: Field): Accessor = {
    ClassDoesNotHaveMembersAccessor(if (target == null) classOf[Null] else target.getClass())
  }
}

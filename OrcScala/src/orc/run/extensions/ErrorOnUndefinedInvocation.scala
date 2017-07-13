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

import orc.InvocationBehavior
import orc.{Handle, Invoker, Accessor}
import orc.values.Format
import orc.error.runtime.UncallableValueException
import orc.values.Field
import orc.error.runtime.{TypeNoSuchMemberException, UncallableTypeException}
import orc.error.runtime.NoSuchMemberException
import orc.UncallableValueInvoker
import orc.DoesNotHaveMembersAccessor

/** @author dkitchin
  */
trait ErrorOnUndefinedInvocation extends InvocationBehavior {
  def getInvoker(target: AnyRef, arguments: Array[AnyRef]): Invoker = {
    UncallableValueInvoker(target)
  }
  
  def getAccessor(target: AnyRef, field: Field): Accessor = {
    DoesNotHaveMembersAccessor(target)
  }
}

//
// InvocationBehaviorUtilities.scala -- Utilities for implementing Invokers and Accessors.
// Project OrcScala
//
// Created by amp on July, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import orc.error.runtime.UncallableValueException
import orc.error.runtime.HaltException
import orc.error.runtime.NoSuchMemberException
import orc.error.runtime.DoesNotHaveMembersException
import java.lang.IllegalArgumentException
import java.lang.IllegalArgumentException

/** A collection of utility methods for writing invokers and accessors.
	*/
object InvocationBehaviorUtilities {
  /** True iff arguments are all of the same type as the matching class in argumentClss.
    *
    * A null value in argumentClss matches only exactly null in arguments.
    */
  def valuesHaveType(arguments: Array[AnyRef], argumentClss: Array[Class[_]]): Boolean = {
    arguments.length == argumentClss.length && {
      // Conceptually: (argumentClss zip arguments).forall(... Predicate ...)
      // But this is hot path, so.... Sorry.
      var i = 0
      var res = true
      while (i < arguments.length && res) {
        // Predicate here:
        res &&= (argumentClss(i) == null && arguments(i) == null) || argumentClss(i).isInstance(arguments(i))
        i += 1
      }
      res
    }
  }

  /** Get the class of v, or null is v == null.
    */
  def valueType(v: AnyRef): Class[_] = {
    if (v == null)
      null
    else
      v.getClass()
  }
}

abstract class OnlyDirectInvoker extends DirectInvoker {
  @throws[UncallableValueException]
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    try {
      h.publish(invokeDirect(target, arguments))
    } catch {
      case _: HaltException =>
        h.halt()
    }
  }
}

/** A invoker sentinel representing the fact that target is not callable.
	*/
case class UncallableValueInvoker(target: AnyRef) extends ErrorInvoker {
  @throws[UncallableValueException]
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    throw new UncallableValueException(target)
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    this.target == target
  }
}

/** A invoker sentinel representing the fact that target is not callable.
	*/
case class IllegalArgumentInvoker(target: AnyRef, arguments: Array[AnyRef]) extends ErrorInvoker {
  @throws[IllegalArgumentException]
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    throw new IllegalArgumentException(s"$target(${arguments.mkString(", ")})")
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    this.target == target && this.arguments == arguments
  }
}

/** A accessor sentinel representing the fact that unknownMember does not exist on the given value.
	*/
case class NoSuchMemberAccessor(target: AnyRef, unknownMember: String) extends ErrorAccessor {
  @throws[NoSuchMemberException]
  def get(target: AnyRef): AnyRef = {
    throw new NoSuchMemberException(target, unknownMember)
  }

  def canGet(target: AnyRef): Boolean = {
    this.target == target
  }
}

/** A accessor sentinel representing the fact that the value does not have members.
	*/
case class DoesNotHaveMembersAccessor(target: AnyRef) extends ErrorAccessor {
  @throws[DoesNotHaveMembersException]
  def get(target: AnyRef): AnyRef = {
    throw new DoesNotHaveMembersException(target)
  }

  def canGet(target: AnyRef): Boolean = {
    this.target == target
  }
}

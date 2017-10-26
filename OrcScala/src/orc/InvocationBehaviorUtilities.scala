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

import orc.error.runtime.{ DoesNotHaveMembersException, HaltException, NoSuchMemberException, UncallableValueException }

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
  def invoke(callContext: CallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    try {
      callContext.publish(invokeDirect(target, arguments))
    } catch {
      case _: HaltException =>
        callContext.halt()
    }
  }
}

/** An invoker sentinel that throws a deferred Exception for this target (for any arg values) */
case class TargetThrowsInvoker(target: AnyRef, e: Throwable) extends ErrorInvoker with DirectInvoker {
  @throws[Throwable]
  def invoke(callContext: CallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    throw e.fillInStackTrace()
  }
  
  @throws[Throwable]
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    throw e.fillInStackTrace()
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    this.target == target
  }
}

/** An invoker sentinel that throws a deferred Exception for this target and arg values */
case class TargetArgsThrowsInvoker(target: AnyRef, arguments: Array[AnyRef], e: Throwable) extends ErrorInvoker with DirectInvoker {
  @throws[Throwable]
  def invoke(callContext: CallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    throw e.fillInStackTrace()
  }
  
  @throws[Throwable]
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    throw e.fillInStackTrace()
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    this.target == target && this.arguments.toSeq == arguments.toSeq
  }
}

/** An invoker sentinel representing the fact that target is not callable.
	*/
case class UncallableValueInvoker(target: AnyRef) extends ErrorInvoker with DirectInvoker {
  @throws[UncallableValueException]
  def invoke(callContext: CallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    throw new UncallableValueException(target)
  }
  
  @throws[UncallableValueException]
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    throw new UncallableValueException(target)
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    this.target == target
  }
}

/** An invoker sentinel representing the fact that arguments are not valid for this call.
	*/
case class IllegalArgumentInvoker(target: AnyRef, arguments: Array[AnyRef]) extends ErrorInvoker with DirectInvoker {
  @throws[IllegalArgumentException]
  def invoke(callContext: CallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    throw new IllegalArgumentException(s"$target(${arguments.mkString(", ")})")
  }
  
  @throws[IllegalArgumentException]
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
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

case class NoSuchMemberOfClassAccessor(cls: Class[_], unknownMember: String) extends ErrorAccessor {
  @throws[NoSuchMemberException]
  def get(target: AnyRef): AnyRef = {
    throw new NoSuchMemberException(target, unknownMember)
  }

  def canGet(target: AnyRef): Boolean = {
    cls.isInstance(target)
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

/** A accessor sentinel representing the fact that the class does not have members.
	*/
case class ClassDoesNotHaveMembersAccessor(targetCls: Class[_]) extends ErrorAccessor {
  @throws[DoesNotHaveMembersException]
  def get(target: AnyRef): AnyRef = {
    throw new DoesNotHaveMembersException(target)
  }

  def canGet(target: AnyRef): Boolean = {
    targetCls.isInstance(target)
  }
}

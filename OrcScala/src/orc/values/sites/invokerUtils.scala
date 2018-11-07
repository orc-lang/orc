//
// invokerUtils.scala -- Utilities for implementing Invokers.
// Project OrcScala
//
// Created by amp on Aug, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.ErrorInvoker
import orc.DirectInvoker
import orc.Invoker
import orc.error.runtime.UncallableValueException

import InvocationBehaviorUtilities._

abstract class TargetValueAndArgumentClassSpecializedInvoker(val target: AnyRef, val argumentClss: Array[Class[_]]) extends Invoker {
  def this(target: AnyRef, initialArgument: Array[AnyRef]) = {
    this(target, initialArgument.map(valueType))
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    ((this.target eq target) || this.target == target) &&
    valuesHaveType(arguments, argumentClss)
  }
}

abstract class TargetClassAndArgumentClassSpecializedInvoker(val targetCls: Class[_], val argumentClss: Array[Class[_]]) extends Invoker {
  def this(initialTarget: AnyRef, initialArgument: Array[AnyRef]) = {
    this(valueType(initialTarget), initialArgument.map(valueType))
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    valueHasType(target, targetCls) &&
    valuesHaveType(arguments, argumentClss)
  }
}

/** An invoker sentinel that throws a deferred Exception for this target and arg values */
class ThrowsInvoker(initialTarget: AnyRef, initialArgument: Array[AnyRef], e: => Throwable) extends
    TargetClassAndArgumentClassSpecializedInvoker(initialTarget, initialArgument) with ErrorInvoker with DirectInvoker {
  @throws[Throwable]
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    throw e
  }
}

/** An invoker sentinel representing the fact that target is not callable.
  */
case class UncallableValueInvoker(target: AnyRef) extends ErrorInvoker with DirectInvoker {
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
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    throw new IllegalArgumentException(s"$target(${arguments.mkString(", ")})")
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    this.target == target && this.arguments == arguments
  }
}

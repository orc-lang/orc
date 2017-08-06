//
// InvocationBehavior.scala -- Interfaces for Orc site invocation
// Project OrcScala
//
// Created by amp on July 12, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import orc.error.runtime.{ DoesNotHaveMembersException, HaltException, NoSuchMemberException, UncallableValueException }
import orc.values.Field

/** An action class implementing invocation for specific target and argument types.
  *
  * The fundamental difference between an Invoker and Accessor is that an accessor can return a future
  * for later forcing, where as an Invoker allows blocking during the call itself. These APIs are
  * mutually encodable, however this encoding would have a significant performance cost.
  * 
  * Invoker.canInvoke must only depend on immutable information in targets and the invoker. This means
  * canInvoke(v) will always return the same value for a specific value v. Similarly, if getInvoker(v) returns 
  * invoker, then invoker.canInvoke(v) must always be true.
  */
trait Invoker {
  /** Return true if InvocationBehavior#getInvoker would return an equivalent
    * instance for these argument types. Equivalent means that for these values
    * invoke would behave the same.
    *
    * This should be as fast as possible. Returning false erroneously is allowed,
    * but may dramatically effect performance on some backends.
    */
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean

  /** Invoke target with the given arguments.
    *
    * If canInvoke(target, arguments) would return false than the behavior of
    * this call is undefined.
    *
    * This call may still throw UncallableValueException even if canInvoke returns true.
    * This could occur for sites which have mutable values which may stop being callable.
    */
  @throws[UncallableValueException]
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit
}

/** Implement direct invocation for calls with do not block and do not need runtime access.
  *
  */
trait DirectInvoker extends Invoker {
  /** Invoke target with the given arguments are returns it's single publication or throws HaltException if
    * the invocation does not publish.
    *
    * This call may not block on external events, but may use locks as needed as long as the locks will
    * be available with relatively low-latency. Any delay in this call may delay the execution of unrelated
    * tasks or threads.
    * 
    * This call may still throw UncallableValueException even if canInvoke returns true.
    * This could occur for sites which have mutable values which may stop being callable.
    *
    * The returned value may not be a future.
    */
  @throws[HaltException]
  @throws[UncallableValueException]
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef
}

/** Type of error sentinels returned by InvocationBehavior.getInvoker.
  *
  * These invokers must NEVER be cached.
  */
trait ErrorInvoker extends Invoker

/** An action class implementing field extraction for a specific target type and field.
  *
  * The fundamental difference between an Invoker and Accessor is that an accessor can return a future
  * for later forcing, where as an Invoker allows blocking during the call itself. These APIs are
  * mutually encodable, however this encoding would have a significant performance cost.
  * 
  * Accessor.canGet must only depend on immutable information in targets and the accessor. This means
  * canGet(v) will always return the same value for a specific value v. Similarly, if getAccessor(v) returns 
  * accessor, then accessor.canGet(v) must always be true.
  */
trait Accessor {
  /** Return true if InvocationBehavior#getAccessor would return an equivalent
    * instance for this target type. Equivalent means that for these values get
    * would behave the same.
    *
    * This should be as fast as possible. Returning false erroniously is allowed,
    * but may dramatically effect performance on some backends.
    */
  def canGet(target: AnyRef): Boolean

  /** Extract the value of the field from target.
    *
    * The returned value may be a future. The caller must check the future and force
    * it when appropriate.
    *
    * If canGet(target) would return false than the behavior of this call is
    * undefined.
    *
    * This call may still throw NoSuchMemberException even if canGet returns true.
    * This occures for types with runtime variable sets of fields meaning that while
    * this is the correct accessor there is no field available.
    */
  @throws[NoSuchMemberException]
  @throws[DoesNotHaveMembersException]
  def get(target: AnyRef): AnyRef
}

/** Type of error sentinels returned by InvocationBehavior.getAccessor.
  *
  * These accessors must NEVER be cached.
  */
trait ErrorAccessor extends Accessor

/** Define invocation behaviors for a runtime
  */
trait InvocationBehavior {
  /** Get an invoker for a specific target type and argment types.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Invoker or DirectInvoker for the given values or an 
    * 			  instance of InvokerError if there is no invoker.
    * 
    * @see UncallableValueInvoker
    */
  def getInvoker(target: AnyRef, arguments: Array[AnyRef]): Invoker

  /** Get an accessor which extracts a given field value from a target.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Accessor for the given classes or an 
    * 			  instance of AccessorError if there is no accessor.
    * 
    * @see NoSuchMemberAccessor, DoesNotHaveMembersAccessor
    */
  def getAccessor(target: AnyRef, field: Field): Accessor
}

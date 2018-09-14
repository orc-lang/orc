//
// InvocationInterceptor.scala -- Scala traits InvocationInterceptor and NoInvocationInterception
// Project PorcE
//
// Created by amp on Aug 15, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

/** Provides a "hook" to intercept external calls from an Execution.
  *
  * @author amp
  */
trait InvocationInterceptor {
  this: PorcEExecution =>

  /** Return true iff the invocation is distributed.
    *
    * This call must be as fast as possible since it is called before every external call. In
    * addition, this method is partially evaluated (inlined) by Truffle unless the implementation
    * is marked with @TruffleBoundary and @noinline (the second one is to prevent Scala 
    * inlining problems).
    */
  def shouldInterceptInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean

  /** Invoke an call remotely as needed by the target and arguments.
    *
    * This will only be called if `shouldInterceptInvocation(target, arguments)` is true.
    */
  def invokeIntercepted(callContext: MaterializedCPSCallContext, target: AnyRef, arguments: Array[AnyRef]): Unit
}

/** Intercept no external calls at all.
  *
  * @author amp
  */
trait NoInvocationInterception extends InvocationInterceptor {
  this: PorcEExecution =>

  override def shouldInterceptInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean = false
  override def invokeIntercepted(callContext: MaterializedCPSCallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    throw new AssertionError("invokeIntercepted called when shouldInterceptInvocation=false")
  }
}

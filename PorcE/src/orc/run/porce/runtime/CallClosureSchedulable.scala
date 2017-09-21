//
// CallClosureSchedulable.scala -- Scala class and object CallClosureSchedulable
// Project PorcE
//
// Created by amp on Aug 11, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.Schedulable
import orc.run.porce.Logger

object CallClosureSchedulable {
  /** Create a schedulable which will call `closure` with no arguments.
    */
  def apply(closure: PorcEClosure): CallClosureSchedulable = {
    varArgs(closure, null)
  }

  /** Create a schedulable which will call `closure` the given argument.
    */
  def apply(closure: PorcEClosure, arg1: AnyRef): CallClosureSchedulable = {
    varArgs(closure, Array(null, arg1))
  }

  /** Create a schedulable which will call `closure` with the given arguments as a normal array.
    *
    * This method has to allocate a new array to perform the call. Use `.varArgs` if you can
    * control the input array from creation.
    */
  def varArgsSlow(closure: PorcEClosure, arguments: Array[AnyRef]): CallClosureSchedulable = {
    val args = Array.ofDim[AnyRef](arguments.length + 1)
    System.arraycopy(arguments, 0, args, 1, arguments.length)
    varArgs(closure, args)
  }

  /** Create a schedulable which will call `closure` the given specially formatted arguments array.
    *
    * The `arguments` array must have `null` as it element 0 and then all the arguments as
    * elements 1 through N.
    */
  def varArgs(closure: PorcEClosure, arguments: Array[AnyRef]): CallClosureSchedulable = {
    new CallClosureSchedulable(closure, arguments)
  } 
  
  @inline
  def simpleCall(closure: PorcEClosure, arguments: Array[AnyRef]) = {
    if (arguments == null)
      closure.callFromRuntime()
    else
      closure.callFromRuntimeArgArray(arguments)
  }

}

final class CallClosureSchedulable private (private var closure: PorcEClosure, private var arguments: Array[AnyRef]) extends Schedulable {
  override val nonblocking: Boolean = true
  
  def run(): Unit = {
    assert(closure != null)
    //Logger.entering(getClass.getName, "run", Seq(hashCode().formatted("%x"), closure, arguments))
    // TODO: This is a Scala implementation of the logic and handling in CatchTailCall. Ideally we could reuse that logic. However to do so we would need access to the runtime and probably execution.
    try {
      val (t, a) = (closure, arguments)
      closure = null
      arguments = null
      CallClosureSchedulable.simpleCall(t, a)
    } catch {
      case e: TailCallException =>
        //Logger.entering(getClass.getName, "run", Seq(CallClosureSchedulable.this.hashCode().formatted("%x"), closure, arguments))
        var exception = e
        while(exception != null) {
          try {
            val (t, a) = (exception.target, exception.arguments)
            exception = null
            //Logger.entering(getClass.getName, "run(tail call)", Seq(CallClosureSchedulable.this.hashCode().formatted("%x"), t, a))
            CallClosureSchedulable.simpleCall(t, null +: a)
          } catch {
            case e: TailCallException =>
              exception = e
          }
        }
    }
    //Logger.exiting(getClass.getName, "run", CallClosureSchedulable.this.hashCode().formatted("%x"))
  }
}

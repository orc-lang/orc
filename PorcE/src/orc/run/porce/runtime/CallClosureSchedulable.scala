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
}

final class CallClosureSchedulable private (closure: PorcEClosure, arguments: Array[AnyRef]) extends Schedulable {
  def run(): Unit = {
    if (arguments == null)
      closure.callFromRuntime()
    else
      closure.callFromRuntimeArgArray(arguments)
  }
}

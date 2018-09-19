//
// CallClosureSchedulable.scala -- Scala class and object CallClosureSchedulable
// Project PorcE
//
// Created by amp on Aug 11, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.Schedulable
import orc.values.Format

object CallClosureSchedulable {
  /** Create a schedulable which will call `closure` with no arguments.
    */
  def apply(closure: PorcEClosure, execution: PorcEExecution): CallClosureSchedulable = {
    varArgs(closure, null, execution)
  }

  /** Create a schedulable which will call `closure` the given argument.
    */
  def apply(closure: PorcEClosure, arg1: AnyRef, execution: PorcEExecution): CallClosureSchedulable = {
    varArgs(closure, Array(null, arg1), execution)
  }

  /** Create a schedulable which will call `closure` with the given arguments as a normal array.
    *
    * This method has to allocate a new array to perform the call. Use `.varArgs` if you can
    * control the input array from creation.
    */
  def varArgsSlow(closure: PorcEClosure, arguments: Array[AnyRef], execution: PorcEExecution): CallClosureSchedulable = {
    val args = Array.ofDim[AnyRef](arguments.length + 1)
    System.arraycopy(arguments, 0, args, 1, arguments.length)
    varArgs(closure, args, execution)
  }

  /** Create a schedulable which will call `closure` the given specially formatted arguments array.
    *
    * The `arguments` array must have `null` as it element 0 and then all the arguments as
    * elements 1 through N.
    */
  def varArgs(closure: PorcEClosure, arguments: Array[AnyRef], execution: PorcEExecution): CallClosureSchedulable = {
    new CallClosureSchedulable(closure, arguments, execution)
  }
}

final class CallClosureSchedulable private (private var _closure: PorcEClosure, private var _arguments: Array[AnyRef], execution: PorcEExecution) extends Schedulable {
  override val nonblocking: Boolean = true

  /*override val priority: Int = {
    val t = closure.getTimePerCall
    if (t < Long.MaxValue) {
      (t / 1000 min Int.MaxValue).toInt
    } else {
      100
    }
  }*/

  def closure = _closure

  def arguments = _arguments

  def run(): Unit = {
    val (t, a) = (_closure, _arguments)
    _closure = null
    _arguments = null
    if (a == null)
      execution.invokeClosure(t, Array(null))
    else
      execution.invokeClosure(t, a)
  }

  override def toString(): String = {
    val a = if (PorcERuntime.displayClosureValues) {
      if (arguments == null) "" else {
        arguments.map(v => Format.formatValue(v).take(48)).mkString(", ")
      }
    } else {
      ""
    }

    s"$closure($a)" // %$priority
  }
}

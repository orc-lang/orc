//
// Accessors.scala -- Scala objects GetFieldSite, GetMethodSite
// Project OrcScala
//
// Created by amp on Aug 19, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.builtin

import orc.{ Accessor, ErrorAccessor, FutureReader, Handle, Invoker, OnlyDirectInvoker, OrcRuntime }
import orc.util.ArrayExtensions.{ Array1, Array2 }
import orc.values.Field
import orc.values.sites.InvokerMethod

object GetFieldSite extends InvokerMethod {
  val arity = 2
  override def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    args match {
      case Array2(o, f: Field) =>
        val accessor = runtime.getAccessor(o, f)
        new GetFieldInvoker(accessor, f)
    }
  }
}

class GetFieldInvoker(accessor: Accessor, f: Field) extends Invoker {
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    target == GetFieldSite && arguments.length == 2 && f == arguments(1) && accessor.canGet(arguments(0))
  }
  
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    val v = accessor.get(arguments(0))
    v match {
      case f: orc.Future =>
        f.read(new FutureHandler(h))
      case _ =>
        h.publish(v)
    }
  }
}

object GetMethodSite extends InvokerMethod {
  val arity = 2
  override def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    args match {
      case Array1(o) =>
        val accessor = runtime.getAccessor(o, Field("apply"))
        accessor match {
          case _: ErrorAccessor =>
            new GetMethodPassthroughInvoker(accessor)
          case _ =>
            new GetMethodApplyInvoker(accessor)
        }
    }
  }
}

class GetMethodPassthroughInvoker(accessor: Accessor) extends OnlyDirectInvoker {
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    target == GetMethodSite && arguments.length == 1 && accessor.canGet(arguments(0))
  }
  
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    arguments(0)
  }
}

class GetMethodApplyInvoker(accessor: Accessor) extends Invoker {
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    target == GetMethodSite && arguments.length == 1 && accessor.canGet(arguments(0))
  }
  
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    val v = accessor.get(arguments(0))
    v match {
      case f: orc.Future =>
        f.read(new FutureHandler(h))
      case _ =>
        h.publish(v)
    }
  }
}

class FutureHandler(h: Handle) extends FutureReader {
  def publish(v: AnyRef): Unit = h.publish(v)
  def halt(): Unit = h.halt()
}

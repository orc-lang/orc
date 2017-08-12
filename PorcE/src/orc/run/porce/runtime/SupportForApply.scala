//
// SupportForApply.scala -- Scala trait SupportForApply
// Project OrcScala
//
// Created by amp on July 14, 2017.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.InvocationBehavior
import orc.values.Field
import orc.ErrorAccessor
import orc.Accessor
import orc.Handle
import orc.error.OrcException
import orc.error.runtime.JavaException
import orc.FutureReader
import orc.Invoker

trait SupportForApply extends InvocationBehavior {
  val applyField = Field("apply")
  abstract override def getInvoker(target: AnyRef, arguments: Array[AnyRef]) = {
    val applyAccessor = getAccessor(target, applyField)
    applyAccessor match {
      case _: ErrorAccessor =>
        super.getInvoker(target, arguments)
      case a =>
        //val applyValue = a.get(target)
        //val applyInvoker = getInvoker(applyValue, arguments)
        new AccessThenInvoker(applyAccessor, this)
    }
  }
}

class AccessThenInvoker(val accessor: Accessor, val runtime: InvocationBehavior) extends Invoker {
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    accessor.canGet(target)
  }

  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]) = {
    // TODO: This will probably need to use a cache for the invoker to get better speed.
    def doInvoke(apply: AnyRef) = {
      try {
        runtime.getInvoker(apply, arguments).invoke(h, apply, arguments)
      } catch {
        case e: OrcException =>
          h.halt(e)
        case e: Exception =>
          h.halt(new JavaException(e))
      }
    }

    val applyValue = accessor.get(target)
    applyValue match {
      case f: orc.Future =>
        f.read(new FutureReader {
          def halt() = {
            h.halt()
          }
          def publish(v: AnyRef) = {
            doInvoke(v)
          }
        })
      case v =>
        doInvoke(v)
    }
  }
}

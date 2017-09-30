//
// SupportForSiteInvocation.scala -- Scala trait SupportForSiteInvocation
// Project OrcScala
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.{ CallContext, DirectInvoker, ErrorAccessor, ErrorInvoker, InvocationBehavior, Invoker, OrcRuntime }
import orc.error.OrcException
import orc.error.runtime.{ ExceptionHaltException, HaltException, JavaException }
import orc.values.Field
import orc.values.sites.{ AccessorValue, DirectSite, InvokerMethod, Site }

/** @author dkitchin
  */
trait SupportForSiteInvocation extends InvocationBehavior {
  this: OrcRuntime =>
  abstract override def getInvoker(target: AnyRef, arguments: Array[AnyRef]) = {
    target match {
      case m: InvokerMethod =>
        m.getInvoker(this, arguments) match {
          case ei: ErrorInvoker =>
            val i = super.getInvoker(target, arguments)
            i match {
              case _: ErrorInvoker =>
                // If the super also provides an error then use our error instead since it's probably more detailed.
                ei
              case _ =>
                i
            }
          case i =>
            i
        }
      case ds: DirectSite =>
        new DirectSiteInvoker(ds.getClass())
      case s: Site =>
        new SiteInvoker(s.getClass())
      case _ =>
        super.getInvoker(target, arguments)
    }
  }

  abstract override def getAccessor(target: AnyRef, field: Field) = {
    target match {
      case m: AccessorValue =>
        m.getAccessor(this, field) match {
          case ea: ErrorAccessor =>
            val a = super.getAccessor(target, field)
            a match {
              case _: ErrorAccessor =>
                // If the super also provides an error then use our error instead since it's probably more detailed.
                ea
              case _ =>
                a
            }
          case i =>
            i
        }
      case _ =>
        super.getAccessor(target, field)
    }
  }
}

class SiteInvoker(val siteCls: Class[_ <: Site]) extends Invoker {
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    siteCls.isInstance(target)
  }

  def invoke(callContext: CallContext, target: AnyRef, arguments: Array[AnyRef]) = {
    try {
      siteCls.cast(target).call(arguments, callContext)
    } catch {
      case e: OrcException => callContext.halt(e)
      case e: InterruptedException => throw e
      case e: Exception => callContext.halt(new JavaException(e))
    }
  }
  
  override def toString(): String = s"${getClass.getSimpleName}(${siteCls.getName})" 
}

class DirectSiteInvoker(override val siteCls: Class[_ <: DirectSite]) extends SiteInvoker(siteCls) with DirectInvoker {
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]) = {
    try {
      siteCls.cast(target).calldirect(arguments)
    } catch {
      case e: HaltException => throw e
      case e: Exception => throw new ExceptionHaltException(e)
    }
  }
}
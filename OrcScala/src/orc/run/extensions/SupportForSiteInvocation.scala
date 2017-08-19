//
// SupportForSiteInvocation.scala -- Scala trait SupportForSiteInvocation
// Project OrcScala
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.InvocationBehavior
import orc.Handle
import orc.values.sites.Site
import orc.error.OrcException
import orc.error.runtime.JavaException
import orc.values.sites.DirectSite
import orc.values.Field
import orc.Invoker
import orc.DirectInvoker
import orc.error.runtime.ExceptionHaltException
import orc.error.runtime.HaltException
import orc.values.sites.InvokerMethod
import orc.error.runtime.UncallableValueException
import orc.error.runtime.NoSuchMemberException
import orc.error.runtime.DoesNotHaveMembersException
import orc.values.sites.AccessorValue
import orc.ErrorInvoker
import orc.ErrorAccessor
import orc.OrcRuntime

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
        new DirectSiteInvoker(ds)
      case s: Site =>
        new SiteInvoker(s)
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

class SiteInvoker(val site: Site) extends Invoker {
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    (target eq site) || (target == site)
  }

  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]) = {
    try {
      site.call(arguments, h)
    } catch {
      case e: OrcException => h.halt(e)
      case e: InterruptedException => throw e
      case e: Exception => h.halt(new JavaException(e))
    }
  }
}

class DirectSiteInvoker(override val site: DirectSite) extends SiteInvoker(site) with DirectInvoker {
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]) = {
    try {
      site.calldirect(arguments)
    } catch {
      case e: HaltException => throw e
      case e: Exception => throw new ExceptionHaltException(e)
    }
  }
}
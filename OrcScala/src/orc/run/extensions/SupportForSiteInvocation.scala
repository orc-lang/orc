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

import orc.{ DirectInvoker, ErrorAccessor, ErrorInvoker, InvocationBehavior, Invoker, OrcRuntime }
import orc.values.{ HasMembers, Field }
import orc.values.sites.{ Site }

/** @author dkitchin
  */
trait SupportForSiteInvocation extends InvocationBehavior {
  this: OrcRuntime =>
  abstract override def getInvoker(target: AnyRef, arguments: Array[AnyRef]) = {
    target match {
      case m: Site =>
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
      case _ =>
        super.getInvoker(target, arguments)
    }
  }

  abstract override def getAccessor(target: AnyRef, field: Field) = {
    target match {
      case m: HasMembers =>
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

//
// SupportForSiteInvocation.scala -- Scala trait SupportForSiteInvocation
// Project OrcScala
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
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

/** @author dkitchin
  */
trait SupportForSiteInvocation extends InvocationBehavior {

  override def invoke(h: Handle, v: AnyRef, vs: Array[AnyRef]) {
    v match {
      case (s: Site) =>
        try {
          s.call(vs, h)
        } catch {
          case e: OrcException => h !! e
          case e: InterruptedException => throw e
          case e: Exception => h !! new JavaException(e)
        }
      case _ => super.invoke(h, v, vs)
    }
  }

}

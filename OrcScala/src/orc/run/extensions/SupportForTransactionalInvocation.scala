//
// SupportForSiteInvocation.scala -- Scala trait SupportForSiteInvocation
// Project OrcScala
//
// $Id: SupportForSiteInvocation.scala 2775 2011-04-20 01:30:01Z jthywissen $
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.InvocationBehavior
import orc.{Handle, TransactionalHandle}
import orc.values.sites.{Site, TransactionalSite}
import orc.error.OrcException
import orc.error.runtime.JavaException

/**
 * 
 *
 * @author dkitchin
 */
trait SupportForTransactionalInvocation extends InvocationBehavior {  
  override def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) {
    (v, h) match {
      case (ts: TransactionalSite, th: TransactionalHandle) => 
        try {
          ts.call(vs, th, th.context) // No abort capability yet.
        } catch {
          case e: OrcException => th !! e
          case e: InterruptedException => throw e
          case e: Exception => th !! new JavaException(e)
        }
      case (_, _ : TransactionalHandle) => {
        // FIXME: Make this error more specific
        throw new AssertionError("Can't call a nontransactional site in a transactional context.")
      }
      case _ => super.invoke(h, v, vs)
    }
  }
}
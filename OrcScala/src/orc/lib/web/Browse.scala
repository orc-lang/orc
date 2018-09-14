//
// Browse.scala -- Scala object Browse
// Project OrcScala
//
// Created by dkitchin on Jan 18, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import java.net.URL

import orc.{ OrcEvent }
import orc.error.runtime.ArgumentTypeMismatchException
import orc.types.{ JavaObjectType, OverloadedType, SignalType, SimpleFunctionType, StringType }
import orc.values.Signal
import orc.values.sites.{ TypedSite }
import orc.values.sites.compatibility.{ Site1 }
import orc.values.sites.compatibility.CallContext

/** Open a new browser window or tab for the specified URL.
  *
  * @author dkitchin
  */
case class BrowseEvent(val url: URL) extends OrcEvent

object Browse extends Site1 with TypedSite {

  def call(v: AnyRef, callContext: CallContext): Unit = {
    v match {
      case url: URL => {
        callContext.notifyOrc(BrowseEvent(url))
        callContext.publish(Signal)
      }
      case s: String => call(new URL(s), callContext)
      case a => throw new ArgumentTypeMismatchException(0, "URL", if (a != null) a.getClass().toString() else "null")
    }
  }

  def orcType =
    OverloadedType(List(
      SimpleFunctionType(StringType, SignalType),
      SimpleFunctionType(JavaObjectType(classOf[URL]), SignalType)))

}

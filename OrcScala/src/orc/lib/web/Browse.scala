//
// Browse.scala -- Scala object Browse
// Project OrcScala
//
// Created by dkitchin on Jan 18, 2011.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.web

import java.net.URL

import orc.{ OrcEvent, VirtualCallContext }
import orc.error.runtime.ArgumentTypeMismatchException
import orc.types.{ JavaObjectType, OverloadedType, SignalType, SimpleFunctionType, StringType }
import orc.values.Signal
import orc.values.sites.{ LocalSingletonSite, Site1Simple, TypedSite }

/** Open a new browser window or tab for the specified URL.
  *
  * @author dkitchin
  */
case class BrowseEvent(val url: URL) extends OrcEvent

object Browse extends Site1Simple[AnyRef] with TypedSite with Serializable with LocalSingletonSite {

  def eval(callContext: VirtualCallContext, v: AnyRef) = {
    v match {
      case url: URL => {
        callContext.notifyOrc(BrowseEvent(url))
        callContext.publish(Signal)
      }
      case s: String => eval(callContext, new URL(s))
      case a => throw new ArgumentTypeMismatchException(0, "URL", if (a != null) a.getClass().toString() else "null")
    }
  }

  def orcType =
    OverloadedType(List(
      SimpleFunctionType(StringType, SignalType),
      SimpleFunctionType(JavaObjectType(classOf[URL]), SignalType)))

}

//
// Browse.scala -- Scala class/trait/object Browse
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 18, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import orc.values.sites.Site1
import orc.values.sites.TypedSite
import orc.types.OverloadedType
import orc.types.StringType
import orc.types.JavaObjectType
import orc.types.SimpleFunctionType
import orc.types.SignalType
import orc.types.Top
import orc.Handle
import orc.OrcEvent
import orc.error.runtime.ArgumentTypeMismatchException

import java.net.URL

/**
  * Open a new browser window or tab for the specified URL.
  *
  * @author dkitchin
  */

case class BrowseEvent(val url: URL) extends OrcEvent

object Browse extends Site1 with TypedSite {

  def call(v: AnyRef, h: Handle) = {
    v match {
      case url: URL => {
        h.notifyOrc(BrowseEvent(url))
        h.publish()
      }
      case s: String => call(new URL(s), h)
      case a => throw new ArgumentTypeMismatchException(0, "URL", if (a != null) a.getClass().toString() else "null")
    }
  }

  def orcType =
    OverloadedType(List(
      SimpleFunctionType(StringType, SignalType),
      SimpleFunctionType(JavaObjectType(classOf[URL]), SignalType)))

}

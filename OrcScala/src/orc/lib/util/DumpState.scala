//
// DumpState.scala -- Scala object DumpState
// Project OrcScala
//
// Created by amp on Feb 5, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.util

import orc.CallContext
import orc.types.SignalType
import orc.values.sites.{ Site0, TypedSite }
import orc.values.Signal

/** Cause the execution to dump the token state.
  *
  * @author amp
  */
object DumpState extends Site0 with TypedSite {
  def call(callContext: CallContext) {
    callContext.notifyOrc(orc.run.core.DumpState)
    callContext.publish(Signal)
  }

  def orcType() = SignalType
}

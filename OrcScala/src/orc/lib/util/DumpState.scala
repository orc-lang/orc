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

import orc.values.sites.Site
import orc.values.sites.TypedSite
import orc.values.sites.TotalSite0
import java.lang.Iterable
import orc.compile.typecheck.Typeloader
import orc.lib.builtin.structured.ListType
import orc.types.TypeVariable
import orc.types.FunctionType
import orc.types.SimpleFunctionType
import orc.error.runtime.ArgumentTypeMismatchException
import orc.types.SignalType
import orc.Handle
import orc.values.sites.Site0

/** Cause the execution to dump the token state.
  *
  * @author amp
  */
object DumpState extends Site0 with TypedSite {
  def call(h: Handle) {
    h.notifyOrc(orc.run.core.DumpState)
    h.publish()
  }

  def orcType() = SignalType
}

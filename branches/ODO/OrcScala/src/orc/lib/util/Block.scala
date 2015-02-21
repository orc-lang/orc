//
// IterableToStream.scala -- Scala object IterableToStream
// Project OrcScala
//
// $Id$
//
// Created by amp in Feb 5, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
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
import orc.Handle
import orc.values.sites.Site0
import orc.types.Bot

/** A site that just blocks forever. However it does so in a way that the interpreter knows
  * about so it is handled more efficiently and will not prevent hte interpreter from exiting.
  *
  * @author amp
  */
object Block extends Site0 with TypedSite {
  def call(h: Handle) {
    h.discorporate()
  }

  def orcType() = Bot
}

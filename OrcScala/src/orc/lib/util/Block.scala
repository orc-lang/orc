//
// Block.scala -- Scala object Block
// Project OrcScala
//
// Created by amp on Feb 5, 2015.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util

import orc.OrcRuntime
import orc.types.Bot
import orc.values.sites.{ Site0Base, LocalSingletonSite, TypedSite }

/** A site that just blocks forever. However it does so in a way that the interpreter knows
  * about so it is handled more efficiently and will not prevent the interpreter from exiting.
  *
  * @author amp
  */
object Block extends Site0Base with TypedSite with Serializable with LocalSingletonSite {
  def getInvoker(runtime: OrcRuntime) = {
    invoker(this) {  (callContext, _) =>
      callContext.discorporate()
    }
  }

  def orcType() = Bot
}

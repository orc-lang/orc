//
// PorcEContext.scala -- Scala class PorcEContext
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce

import orc.run.porce.runtime.PorcERuntime

final class PorcEContext(final val runtime: PorcERuntime) {
  private[porce] var thread: Thread = null
}

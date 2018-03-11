//
// Utilities.scala -- Scala object Utilities
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.nodes.RootNode

// TODO: This class is probably not needed. Check for uses and potentially remove this.

/** Utility operations for working with PorcE internal objects.
 *  
 */
object Utilities {
  private val emptyArray = Array[AnyRef]()

  def PorcEClosure(r: RootNode): PorcEClosure = {
    val c = Truffle.getRuntime().createCallTarget(r)
    new orc.run.porce.runtime.PorcEClosure(emptyArray, c, false)
  }

  def isDef(v: AnyRef): Boolean = v match {
    case c: PorcEClosure => {
      c.isRoutine
    }
    // TODO: Add support for external defs if we every have them supported in the API.
    case _ =>
      false
  }
}

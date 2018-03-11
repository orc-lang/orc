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
  private[porce] var depth: Int = 0
  
  /** Increment the stack depth and return true if we can continue to extend the stack.
    * 
    * If this returns true the caller must call decrementStackDepth() after it finishes.
    */
  def incrementAndCheckStackDepth() = {
    if (runtime.maxStackDepth > 0) {
      //if (depth.value > PorcERuntime.maxStackDepth / 2)
      //  Logger.log(Level.INFO, s"incr (depth=${depth.value})")
        
      val r = depth < runtime.maxStackDepth
      if (r)
        depth += 1
      r
    } else {
      false
    }
  }

  /** Decrement stack depth.
    *
    * @see incrementAndCheckStackDepth()
    */
  def decrementStackDepth() = {
    if (runtime.maxStackDepth > 0) {
      //if (depth.value > PorcERuntime.maxStackDepth / 2)
      //  Logger.log(Level.INFO, s"decr (depth=${depth.value})")
        
      depth -= 1
    }
  }

  def resetStackDepth() = {
    if (runtime.maxStackDepth > 0) {
      depth = 0
    }
  }
}

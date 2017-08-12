//
// CallTargetManager.scala -- Trait CallTargetManager
// Project PorcE
//
// Created by amp on Aug 11, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import com.oracle.truffle.api.RootCallTarget

/** The interface to map RootCallTargets to/from Ints for serialization.
  *
  * PorcEExecution implements this trait.
  *
  * @author amp
  */
trait CallTargetManager {
  def callTargetToId(callTarget: RootCallTarget): Int
  def idToCallTarget(id: Int): RootCallTarget
}
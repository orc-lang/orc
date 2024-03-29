//
// CallTargetManager.scala -- Scala trait CallTargetManager
// Project PorcE
//
// Created by amp on Aug 11, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import orc.run.porce.runtime.{ Counter, PorcEClosure, Terminator }

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

  def invokeCallTarget(callSiteId: Int, p: PorcEClosure, c: Counter, t: Terminator, target: AnyRef, arguments: Array[AnyRef]): Unit
}

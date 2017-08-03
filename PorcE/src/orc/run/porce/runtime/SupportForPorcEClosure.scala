//
// SupportForApply.scala -- Scala trait SupportForApply
// Project OrcScala
//
// Created by amp on July 14, 2017.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.InvocationBehavior
import orc.Handle
import orc.Invoker

trait SupportForPorcEClosure extends InvocationBehavior {
  abstract override def getInvoker(target: AnyRef, arguments: Array[AnyRef]) = {
    target match {
      case c: PorcEClosure =>
        new PorcEClosureInvoker(c)
      case _ =>
        super.getInvoker(target, arguments)
    }
  }  
}

class PorcEClosureInvoker(val closure: PorcEClosure) extends Invoker {
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    target == closure
  }

  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]) = {
    val handle = h.asInstanceOf[CPSCallResponseHandler]
    val args = Array.ofDim[AnyRef](arguments.length + 3)
    args(0) = handle.p
    args(1) = handle.c
    args(2) = handle.t
    System.arraycopy(arguments, 0, args, 3, arguments.length)
    // Token: passed to closure via args.
    closure.callFromRuntimeVarArgs(args)
  }
}

//
// SupportForJavaObjectInvocation.scala -- Scala trait SupportForJavaObjectInvocation
// Project OrcScala
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.{ Accessor, InvocationBehavior, Invoker }
import orc.values.{ Field, OrcValue }
import orc.values.sites.JavaCall

/** @author dkitchin, amp
  */
trait SupportForJavaObjectInvocation extends InvocationBehavior {
  abstract override def getInvoker(target: AnyRef, arguments: Array[AnyRef]): Invoker = {
    target match {
      // Assume anything with the OrcValue marker has implemented everything it needs explicitly
      case v: OrcValue => 
        super.getInvoker(target, arguments)
      case _ =>
        JavaCall.getInvoker(target, arguments).getOrElse(super.getInvoker(target, arguments))
    }
  }
  
  abstract override def getAccessor(target: AnyRef, field: Field): Accessor = {
    target match {
      // Assume anything with the OrcValue marker has implemented everything it needs explicitly
      case v: OrcValue => 
        super.getAccessor(target, field)
      case _ =>
        JavaCall.getAccessor(target, field).getOrElse(super.getAccessor(target, field))
    }
  }
}

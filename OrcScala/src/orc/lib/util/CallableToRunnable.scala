//
// CallableToRunnable.scala -- Scala object CallableToRunnable and CallableToCallable
// Project OrcScala
//
// Created by amd on Feb 11, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.util

import java.util.concurrent.Callable

import orc.Handle
import orc.compile.typecheck.Typeloader
import orc.run.core.ExternalSiteCallHandle
import orc.run.extensions.SupportForCallsIntoOrc
import orc.types.{ Bot, FunctionType, SimpleFunctionType, TypeVariable }
import orc.values.sites.{ Site1, TypedSite }

/** Convert an Orc callable into a Java Runnable instance.
  * @author amp
  */
object CallableToRunnable extends Site1 with TypedSite {
  def call(arg: AnyRef, h: Handle) = {
    val runtime = h.asInstanceOf[ExternalSiteCallHandle].caller.execution match {
      case r: SupportForCallsIntoOrc => r
      case _ => throw new AssertionError("CallableToRunnable only works with a runtime that includes SupportForCallsIntoOrc.")
    }
    h.publish(new Runnable {
      def run() {
        runtime.callOrcCallable(arg, Nil)
      }
    })
  }

  def orcType() = {
    val Runnable = Typeloader.liftJavaClassType(classOf[Runnable])
    FunctionType(List(), List(SimpleFunctionType(Bot)), Runnable)
  }

}

/** Convert an Orc callable into a Java Callable instance.
  * @author amp
  */
object CallableToCallable extends Site1 with TypedSite {
  def call(arg: AnyRef, h: Handle) = {
    val runtime = h.asInstanceOf[ExternalSiteCallHandle].caller.execution match {
      case r: SupportForCallsIntoOrc => r
      case _ => throw new AssertionError("CallableToRunnable only works with a runtime that includes SupportForCallsIntoOrc.")
    }
    h.publish(new Callable[AnyRef] {
      def call() = {
        runtime.callOrcCallable(arg, Nil).getOrElse(null)
      }
    })
  }

  def orcType() = {
    val X = new TypeVariable()
    val Callable = Typeloader.liftJavaTypeOperator(classOf[Callable[_]])
    FunctionType(List(), List(SimpleFunctionType(X)), Callable(X))
  }

}

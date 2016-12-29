//
// CallableToRunnable.scala -- Scala object CallableToRunnable and CallableToCallable
// Project OrcScala
//
// $Id$
//
// Created by amd on Feb 11, 2015.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.util

import orc.values.sites.TypedSite
import java.lang.Iterable
import orc.compile.typecheck.Typeloader
import orc.lib.builtin.structured.ListType
import orc.types.TypeVariable
import orc.types.FunctionType
import orc.types.SimpleFunctionType
import orc.error.runtime.ArgumentTypeMismatchException
import orc.types.Bot
import orc.values.sites.Site1
import orc.Handle
import orc.run.extensions.SupportForCallsIntoOrc
import java.util.concurrent.Callable

/** Convert an Orc callable into a Java Runnable instance.
  * @author amp
  */
object CallableToRunnable extends Site1 with TypedSite {
  def call(arg: AnyRef, h: Handle) = {
    val runtime = h.execution match {
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
    val runtime = h.execution match {
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

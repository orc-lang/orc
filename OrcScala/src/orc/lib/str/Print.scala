//
// Print.scala -- Scala object Print, and class PrintEvent
// Project OrcScala
//
// Created by dkitchin on Jan 18, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.str

import orc.{ OrcEvent, OrcRuntime, SiteResponseSet, VirtualCallContext }
import orc.types.{ SignalType, SimpleFunctionType, StringType, Top }
import orc.values.Format.formatValue
import orc.values.Signal
import orc.values.sites.{ Range, Site1Base, TotalSite1Simple, TypedSite }


/** Display a value on the console or equivalent output device.
  *
  * @author dkitchin
  */
case class PrintEvent(val text: String) extends OrcEvent

abstract class PrintSite extends Site1Base[AnyRef] with TypedSite {
  def formatToPrint(v: AnyRef): String =
    v match {
      case s: String => s
      case t => formatValue(t)
    }

  def getInvoker(runtime: OrcRuntime, arg1: AnyRef) = {
    invoker(this, arg1)((ctx, _, a) => call(ctx, a))
  }

  def call(callContext: VirtualCallContext, a: AnyRef): SiteResponseSet

  def orcType = SimpleFunctionType(Top, SignalType)

  override def publications: Range = super.publications intersect Range(1, 1)
}

object Print extends PrintSite {
  def call(callContext: VirtualCallContext, a: AnyRef) = {
    callContext.notifyOrc(PrintEvent(formatToPrint(a)))
    callContext.publish(Signal)
  }
}

object Println extends PrintSite {
  def call(callContext: VirtualCallContext, a: AnyRef) = {
    callContext.notifyOrc(PrintEvent(formatToPrint(a) + "\n"))
    callContext.publish(Signal)
  }
}

object Write extends TotalSite1Simple[AnyRef] {
  def eval(e: AnyRef) = {
    formatValue(e)
  }

  def orcType = SimpleFunctionType(Top, StringType)
}


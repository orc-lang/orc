//
// CanvasImpl.scala -- Java class CanvasImpl
// Project OrcSites
//
// Created by amp on Feb 15, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.gui

import orc.values.sites.compatibility.CallContext
import orc.run.core.ExternalSiteCallController
import orc.run.extensions.SupportForCallsIntoOrc
import orc.values.{ Field }
import orc.values.sites.compatibility.{ Site1, HasMembers }

import javax.swing.DefaultListModel

/** @author amp
  */
class ListModelImpl extends DefaultListModel[AnyRef] {
  def elementsUpdated() = fireContentsChanged(this, 0, getSize())

  def setExtend(i: Int, e: AnyRef) = {
    while (size() <= i) addElement("")
    set(i, e)
  }
}

class ToStringAdapter(val deligate: HasMembers, val execution: SupportForCallsIntoOrc) extends ListenerAdapter {
  val ToStringField = Field("toString")
  override def toString = {
    if (deligate hasMember ToStringField)
      try {
        execution.callOrcMethod(deligate, ToStringField, List()).get.asInstanceOf[String]
      } catch {
        case _: ClassCastException | _: NoSuchElementException => deligate.toOrcSyntax()
      }
    else
      deligate.toOrcSyntax()
  }
}

object ToStringAdapter extends Site1 {
  def call(arg: AnyRef, callContext: CallContext) = {
    val execution = callContext.asInstanceOf[ExternalSiteCallController].caller.execution match {
      case r: SupportForCallsIntoOrc => r
      case _ => throw new AssertionError("CallableToRunnable only works with a runtime that includes SupportForCallsIntoOrc.")
    }
    arg match {
      case d: HasMembers => callContext.publish(new ToStringAdapter(d, execution))
      case o => callContext.publish(o)
    }
  }
}

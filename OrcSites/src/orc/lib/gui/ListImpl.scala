//
// CanvasImpl.scala -- Java class CanvasImpl
// Project OrcSites
//
// Created by amp on Feb 15, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.gui

import javax.swing.ListModel
import javax.swing.DefaultListModel
import orc.run.extensions.SupportForCallsIntoOrc
import orc.values.OrcObjectInterface
import orc.values.Field
import orc.Handle
import orc.values.sites.Site1

/**
  * @author amp
  */
class ListModelImpl extends DefaultListModel[AnyRef] {
  def elementsUpdated() = fireContentsChanged(this, 0, getSize())
  
  def setExtend(i : Int, e : AnyRef) = {
    while(size() <= i) addElement("")
    set(i, e)
  }
}

class ToStringAdapter(val deligate: OrcObjectInterface, val execution: SupportForCallsIntoOrc) extends ListenerAdapter {
  val ToStringField = Field("toString")
  override def toString = {
    if (deligate contains ToStringField)
      try {
        execution.callOrcMethod(deligate, ToStringField, List()).get.asInstanceOf[String]
      } catch {
        case _ : ClassCastException | _ : NoSuchElementException => deligate.toOrcSyntax()
      }
    else 
      deligate.toOrcSyntax()
  }
}

object ToStringAdapter extends Site1 {
  def call(arg: AnyRef, h: Handle) = {
    val execution = h.execution match {
      case r: SupportForCallsIntoOrc => r
      case _ => throw new AssertionError("CallableToRunnable only works with a runtime that includes SupportForCallsIntoOrc.")
    }
    val del = arg match {
      case d: OrcObjectInterface => h.publish(new ToStringAdapter(d, execution))
      case o => h.publish(o)
    }
  }
} 
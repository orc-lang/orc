//
// ListenerAdapters.scala -- Scala Listener Adapter classes
// Project OrcSites
//
// $Id$
//
// Created by amp on Feb 13, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.gui

import java.awt.event.ActionListener
import orc.values.OrcObjectInterface
import java.awt.event.ActionEvent
import orc.run.extensions.SupportForCallsIntoOrc
import orc.OrcRuntime
import orc.values.Field
import orc.values.sites.Site1
import orc.values.sites.TypedSite
import orc.Handle
import orc.compile.typecheck.Typeloader
import orc.ast.oil.named.FunctionType
import orc.error.runtime.ArgumentTypeMismatchException
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener
import java.awt.event.WindowFocusListener

abstract class ListenerAdapter {
  val deligate: OrcObjectInterface
  val runtime: SupportForCallsIntoOrc
  
  def call(f: Field, arguments: List[AnyRef]) = {
    if (deligate contains f) 
      runtime.callOrcMethod(deligate, f, arguments)
  }
}

// TODO: Make this typed once we have object types.
abstract class ListenerAdapterSite extends Site1 {
  def call(arg: AnyRef, h: Handle) = {
    val runtime = h.runtime match {
      case r : SupportForCallsIntoOrc => r 
      case _ => throw new AssertionError("CallableToRunnable only works with a runtime that includes SupportForCallsIntoOrc.")
    }
    val del = arg match {
      case d : OrcObjectInterface => d
      case a => throw new ArgumentTypeMismatchException(0, "OrcObject", if (a != null) a.getClass().toString() else "null")
    }
    h.publish(buildAdapter(runtime, del))
  }

  def buildAdapter(runtime: SupportForCallsIntoOrc, del: OrcObjectInterface): AnyRef
}

class ActionListenerAdapter(val deligate: OrcObjectInterface, val runtime: SupportForCallsIntoOrc) extends ListenerAdapter with ActionListener {
  def actionPerformed(e: ActionEvent) = {
    call(Field("actionPerformed"), List(e))
  }
}
object ActionListenerAdapter extends ListenerAdapterSite {
  def buildAdapter(runtime: SupportForCallsIntoOrc, del: OrcObjectInterface): AnyRef = {
    new ActionListenerAdapter(del, runtime)
  }
}

class WindowListenerAdapter(val deligate: OrcObjectInterface, val runtime: SupportForCallsIntoOrc) 
    extends ListenerAdapter with WindowListener with WindowFocusListener with WindowStateListener {
  // Members declared in java.awt.event.WindowFocusListener   
  def windowGainedFocus(e: java.awt.event.WindowEvent): Unit = call(Field("windowGainedFocus"), List(e))   
  def windowLostFocus(e: java.awt.event.WindowEvent): Unit = call(Field("windowLostFocus"), List(e))      
  // Members declared in java.awt.event.WindowListener   
  def windowActivated(e: java.awt.event.WindowEvent): Unit = call(Field("windowActivated"), List(e))   
  def windowClosed(e: java.awt.event.WindowEvent): Unit = call(Field("windowClosed"), List(e))   
  def windowClosing(e: java.awt.event.WindowEvent): Unit = call(Field("windowClosing"), List(e))   
  def windowDeactivated(e: java.awt.event.WindowEvent): Unit = call(Field("windowDeactivated"), List(e))   
  def windowDeiconified(e: java.awt.event.WindowEvent): Unit = call(Field("windowDeiconified"), List(e))   
  def windowIconified(e: java.awt.event.WindowEvent): Unit = call(Field("windowIconified"), List(e))   
  def windowOpened(e: java.awt.event.WindowEvent): Unit = call(Field("windowOpened"), List(e))     
  // Members declared in java.awt.event.WindowStateListener   
  def windowStateChanged(e: java.awt.event.WindowEvent): Unit = call(Field("windowStateChanged"), List(e))
}
object WindowListenerAdapter extends ListenerAdapterSite {
  def buildAdapter(runtime: SupportForCallsIntoOrc, del: OrcObjectInterface): AnyRef = {
    new WindowListenerAdapter(del, runtime)
  }
}
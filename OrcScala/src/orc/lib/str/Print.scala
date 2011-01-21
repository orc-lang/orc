//
// Print.scala -- Scala class/trait/object Print
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 18, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.str

import orc.values.sites.Site1
import orc.values.sites.TypedSite
import orc.types.SimpleFunctionType
import orc.types.SignalType
import orc.types.Top
import orc.Handle
import orc.OrcEvent

/**
 * 
 * Display a value on the console or equivalent output device.
 *
 * @author dkitchin
 */

case class PrintEvent(val text: String) extends OrcEvent

object Print extends Site1 with TypedSite {

  def call(a: AnyRef, h: Handle) = {
    h.notifyOrc(PrintEvent(a.toString()))
    h.publish()
  }
  
  def orcType = SimpleFunctionType(Top, SignalType)
  
}
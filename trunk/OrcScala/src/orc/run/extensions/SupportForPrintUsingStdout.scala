//
// SupportForStdout.scala -- Scala trait SupportForStdout
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import orc.OrcEvent
import orc.lib.str.PrintEvent

/**
 * 
 * Direct printed strings to standard output.
 *
 * @author dkitchin
 */
trait SupportForPrintUsingStdout extends Orc {
  
  override def generateOrcHandlers(host: Execution): List[OrcHandler] = {
    val thisHandler = { 
      case PrintEvent(text) => System.out.print(text)
    } : PartialFunction[OrcEvent, Unit]
    
    thisHandler :: super.generateOrcHandlers(host)
  }
}

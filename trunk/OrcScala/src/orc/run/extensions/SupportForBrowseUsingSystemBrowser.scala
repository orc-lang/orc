//
// SupportForStdout.scala -- Scala trait SupportForStdout
// Project OrcScala
//
// $Id: SupportForStdout.scala 2228 2010-12-07 19:13:50Z jthywissen $
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
import orc.lib.web.BrowseEvent

/**
 * 
 * Open a web page using the system web browser, if available.
 *
 * @author dkitchin
 */
trait SupportForBrowseUsingSystemBrowser extends Orc {
  
  override def generateOrcHandlers(host: Execution): List[OrcHandler] = {
    val thisHandler = { 
      case BrowseEvent(url) => {
        java.awt.Desktop.getDesktop().browse(url.toURI())
      }
    } : PartialFunction[OrcEvent, Unit]
    
    thisHandler :: super.generateOrcHandlers(host)
  }
}

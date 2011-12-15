//
// OrcDesktopEventAction.scala -- Scala class OrcDesktopEventAction and trait OrcDesktopActions
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 24, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run

import orc.OrcEventAction
import orc.OrcEvent
import orc.script.SwingBasedPrompt
import orc.lib.util.PromptEvent
import orc.lib.str.PrintEvent
import orc.lib.web.BrowseEvent

/**
  *
  * @author dkitchin
  */

class OrcDesktopEventAction extends OrcEventAction with OrcDesktopActions

trait OrcDesktopActions extends OrcEventAction {

  @throws(classOf[Exception])
  override def other(event: OrcEvent) {
    event match {
      case PrintEvent(text) => {
        Console.out.print(text)
        Console.out.flush()
      }
      case PromptEvent(prompt, callback) => {
        val response = SwingBasedPrompt.runPromptDialog("Orc", prompt)
        if (response != null) {
          callback.respondToPrompt(response)
        } else {
          callback.cancelPrompt()
        }
      }
      case BrowseEvent(url) => {
        java.awt.Desktop.getDesktop().browse(url.toURI())
      }
      case e => super.other(e)
    }
  }

}

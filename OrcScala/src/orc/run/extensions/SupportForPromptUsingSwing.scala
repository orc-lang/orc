//
// SupportForPromptUsingSwing.scala -- Scala class/trait/object SupportForPromptUsingSwing
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 20, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import orc.OrcEvent
import orc.lib.util.PromptEvent

/**
 * 
 *
 * @author dkitchin
 */
trait SupportForPromptUsingSwing extends Orc {

  override def generateOrcHandlers(host: Execution): List[OrcHandler] = {
    val thisHandler = { 
      case PromptEvent(prompt, callback) => {
        val response = SwingBasedPrompt.runPromptDialog("Orc", prompt)
        if (response != null) {
          callback.respondToPrompt(response)
        }
        else {
          callback.cancelPrompt()
        }
      }
    } : PartialFunction[OrcEvent, Unit]
    
    thisHandler :: super.generateOrcHandlers(host)
  }
  
  
}
//
// Prompt.scala -- Scala object Prompt, class PromptEvent, and trait PromptCallback
// Project OrcScala
//
// Created by dkitchin on Jan 20, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.util

import orc.{ VirtualCallContext, OrcEvent, OrcRuntime }
import orc.types.{ SimpleFunctionType, StringType }
import orc.values.sites.{ Site1Base, TypedSite }

/** Generic site for presenting the user with a prompt for input.
  * Different runtimes will present different prompts depending
  * on how they handle a PromptEvent.
  *
  * @author dkitchin
  */
trait PromptCallback {
  def respondToPrompt(response: String)
  def cancelPrompt()
}

case class PromptEvent(val prompt: String, val callback: PromptCallback) extends OrcEvent

object Prompt extends Site1Base[String] with TypedSite {

  def getInvoker(runtime: OrcRuntime, arg1: String) = {
    invoker(this, arg1) { (callContext, _, prompt) =>
      val ctx = callContext.materialize()
      val callback = new PromptCallback() {
        def respondToPrompt(response: String) = ctx.publish(response)
        def cancelPrompt() = ctx.halt()
      }
      callContext.notifyOrc(PromptEvent(prompt, callback))
      callContext.empty
    }
  }

  def orcType = SimpleFunctionType(StringType, StringType)
}

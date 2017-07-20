//
// PorcEBackend.scala -- Scala class PorcEBackend
// Project OrcScala
//
// Created by amp on Jun 30, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import orc.ast.porc.MethodCPS
import orc.run.porce.runtime.PorcERuntime
import orc.compiler.porce.PorcToPorcE
import orc.run.porce.runtime.PorcEExecution
import orc.run.porce.runtime.PorcEExecutionRef
import orc.run.porce.runtime.PorcEExecutionHolder
import orc.run.porce.runtime.PorcEClosure
import orc.run.porce.Logger

/** A backend implementation using the Orctimizer and Porc compilers.
  *
  * This is designed to be extended with a runtime which takes Porc as input.
  *
  * @author amp
  */
class PorcEBackend extends PorcBackend {
  def createRuntime(options: OrcExecutionOptions): Runtime[MethodCPS] = new PorcERuntime("PorcE on Truffles") with Runtime[MethodCPS] {
    startScheduler(options)
    val translator = new PorcToPorcE
    
    val cache = new collection.mutable.HashMap[MethodCPS, (PorcEExecutionHolder, PorcEClosure)]()
    
    private def start(ast: MethodCPS, k: orc.OrcEvent => Unit): PorcEExecution = {
      val execution = new PorcEExecution(this, k)
      val porceAst = cache.get(ast) match {
        case Some((holder, porceAst)) => {
          if(holder.setExecution(execution)) {
            Logger.info(s"Updating execution for cached program.")
            porceAst
          } else {
            Logger.info(s"Converting and not caching program that is being started concurrently with itself.")
            val executionHolder = new PorcEExecutionHolder(execution)
            val porceAst = translator(ast, executionHolder, this)
            porceAst
          }
        }
        case None => {
          Logger.info(s"Converting and caching input program.")
          val executionHolder = new PorcEExecutionHolder(execution)
          val porceAst = translator(ast, executionHolder, this)
          cache += ((ast, (executionHolder, porceAst)))
          porceAst
        }
      }
      addRoot(execution)
      execution.scheduleProgram(porceAst)
      execution
    }

    def run(ast: MethodCPS, k: orc.OrcEvent => Unit): Unit = start(ast, k)
    def runSynchronous(ast: MethodCPS, k: orc.OrcEvent => Unit): Unit = start(ast, k).waitForHalt()
  }
}

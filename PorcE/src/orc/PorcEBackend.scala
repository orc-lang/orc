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
import orc.compiler.porce.PorcToPorcE
import orc.run.porce.runtime.{ PorcEExecution, PorcEExecutionHolder, PorcEExecutionWithLaunch, PorcERuntime }
import orc.run.porce.PorcELanguage

case class PorcEBackendType() extends BackendType {
  type CompiledCode = MethodCPS

  val name = "porc"
  def newBackend(): Backend[MethodCPS] = new PorcEBackend()
}

/** A backend implementation using the Orctimizer and Porc compilers.
  *
  * This is designed to be extended with a runtime which takes Porc as input.
  *
  * @author amp
  */
case class PorcEBackend(language: PorcELanguage = null) extends PorcBackend {
  def createRuntime(options: OrcExecutionOptions): Runtime[MethodCPS] = new PorcERuntime("PorcE on Truffles", language) with Runtime[MethodCPS] {
    startScheduler(options)

    private def start(ast: MethodCPS, k: orc.OrcEvent => Unit): PorcEExecutionWithLaunch = synchronized {
      val execution = new PorcEExecution(this, k) with PorcEExecutionWithLaunch
      val executionHolder = new PorcEExecutionHolder(execution)
      val (porceAst, map) = PorcToPorcE(ast, executionHolder, false, language)
      addRoot(execution)
      execution.scheduleProgram(porceAst, map)
      execution
    }

    def run(ast: MethodCPS, k: orc.OrcEvent => Unit): Unit = start(ast, k)
    def runSynchronous(ast: MethodCPS, k: orc.OrcEvent => Unit): Unit = start(ast, k).waitForHalt()
  }
}

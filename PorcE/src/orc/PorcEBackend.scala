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
import orc.run.porce.runtime.{ PorcEExecution, PorcEExecutionWithLaunch, PorcERuntime }
import orc.run.porce.PorcELanguage

case class PorcEBackendType() extends BackendType {
  type CompiledCode = MethodCPS

  val name = "porc"
  def newBackend(): Backend[MethodCPS] = new PorcEBackend()
}

object PorcEBackend {
  private def setSystemPropertyIfUnset(k: String, v: Any) = {
    if (System.getProperty(k) == null)
      System.setProperty(k, v.toString)
  }

  def loadOptOpts(options: OrcCompilationOptions) = {
    // Allow inline some spawns into there spawn site instead of calling them:
    setSystemPropertyIfUnset(
        "orc.porce.allowSpawnInlining",
        options.optimizationFlags("porce:inline-spawn").asBool(true))

    // Only inlining fast tasks (based on runtime profiling):
    setSystemPropertyIfUnset(
        "orc.porce.inlineAverageTimeLimit",
        options.optimizationFlags("porce:inline-average-time-limit").asString("0.1"))

    // Polymorphic inline caches for calls:
    if (!options.optimizationFlags("porce:polymorphic-inline-caching").asBool(true)) {
      setSystemPropertyIfUnset("orc.porce.cache.getFieldMaxCacheSize", 0)
      setSystemPropertyIfUnset("orc.porce.cache.internalCallMaxCacheSize", 0)
      setSystemPropertyIfUnset("orc.porce.cache.externalDirectCallMaxCacheSize", 0)
      setSystemPropertyIfUnset("orc.porce.cache.externalCPSCallMaxCacheSize", 0)
      setSystemPropertyIfUnset("orc.porce.optimizations.externalCPSDirectSpecialization", false)
    }

    // Specialize compiled code for runtime states such as futured already being resolved:
    if (!options.optimizationFlags("porce:specialization").asBool(true)) {
      setSystemPropertyIfUnset("orc.porce.optimizations.inlineForceResolved", false)
      setSystemPropertyIfUnset("orc.porce.optimizations.inlineForceHalted", false)
      setSystemPropertyIfUnset("orc.porce.optimizations.specializeOnCounterStates", false)
      setSystemPropertyIfUnset("orc.porce.optimizations.environmentCaching", false)
    }
  }
}

/** A backend implementation using the Orctimizer and Porc compilers.
  *
  * This is designed to be extended with a runtime which takes Porc as input.
  *
  * @author amp
  */
case class PorcEBackend(language: PorcELanguage = null) extends PorcBackend {
  override def modifyCompilationOptions(options: OrcCompilationOptions): OrcCompilationOptions = {
    PorcEBackend.loadOptOpts(options)
    options
  }

  def createRuntime(options: OrcExecutionOptions): Runtime[MethodCPS] = new PorcERuntime("PorcE on Truffles", language) with Runtime[MethodCPS] {
    startScheduler(options)

    private def start(ast: MethodCPS, k: orc.OrcEvent => Unit): PorcEExecutionWithLaunch = synchronized {
      val execution = new PorcEExecution(this, k) with PorcEExecutionWithLaunch
      val (porceAst, map) = PorcToPorcE.method(ast, execution, language)
      addRoot(execution)
      execution.scheduleProgram(porceAst, map)
      execution
    }

    def run(ast: MethodCPS, k: orc.OrcEvent => Unit): Unit = start(ast, k)
    def runSynchronous(ast: MethodCPS, k: orc.OrcEvent => Unit): Unit = start(ast, k).waitForHalt()
  }
}

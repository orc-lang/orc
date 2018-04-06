//
// PorcEDistributedBackend.scala -- Scala class DistributedBackend
// Project OrcScala
//
// Created by jthywiss on Dec 20, 2016.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import orc.ast.porc.MethodCPS
import orc.run.distrib.porce.LeaderRuntime

case class PorcEDistributedBackendType() extends BackendType {
  type CompiledCode = MethodCPS

  val name = "porc-distrib"
  def newBackend(): Backend[MethodCPS] = new PorcEDistributedBackend()
}

/** A backend implementation using the dOrc interpreter.
  *
  * @author amp, jthywiss
  */
class PorcEDistributedBackend extends PorcBackend {
  override def modifyCompilationOptions(options: OrcCompilationOptions): OrcCompilationOptions = {
    val oos = options.optimizationOptions
    // Add options to prevent the use of features which are incompatible with DOrc.
    oos.add("porc:directgetfields=false")
    oos.add("porc:directcalls=false")
    oos.add("porc:usegraft=false")
    options.optimizationOptions = oos
    
    PorcEBackend.loadOptOpts(options)
    
    options
  }
  
  def createRuntime(options: OrcExecutionOptions): Runtime[MethodCPS] = new LeaderRuntime() with Runtime[MethodCPS] {
    def run(ast: MethodCPS, k: orc.OrcEvent => Unit): Unit = run(ast, k, options)
    def runSynchronous(ast: MethodCPS, k: orc.OrcEvent => Unit): Unit = runSynchronous(ast, k, options)
  }
}

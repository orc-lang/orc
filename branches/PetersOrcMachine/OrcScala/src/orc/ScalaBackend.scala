//
// StandardBackend.scala -- Scala class/trait/object StandardBackend
// Project OrcScala
//
// $Id$
//
// Created by amp on Aug 28, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import orc.ast.oil.nameless.Expression
import orc.compile.StandardOrcCompiler
import orc.ast.oil.xml.OrcXML
import orc.run.StandardOrcRuntime
import orc.compile.parse.OrcInputContext
import orc.error.compiletime.CompileLogger
import orc.progress.ProgressMonitor
import orc.error.loadtime.LoadingException
import java.io.OutputStreamWriter
import orc.run.compiled.{Runtime => RunCompiledRuntime}
import orc.compile.ScalaOrcCompiler
import orc.run.compiled.ExecutionHandle
import orc.run.compiled.ScalaRuntimeCompiler
import orc.run.compiled.OrcModule

/**
  *
  * @author amp
  */
class ScalaBackend extends Backend[OrcModule] {
  type CodeType = OrcModule
  
  lazy val compiler: Compiler[CodeType] = new ScalaOrcCompiler() with Compiler[CodeType] {
    def compile(source: OrcInputContext, options: OrcCompilationOptions, 
        compileLogger: CompileLogger, progress: ProgressMonitor): CodeType = { 
      val (src, bindings) = this(source, options, compileLogger, progress)
      scalaCompiler.compile(src, bindings)
    }
  }
  
  val serializer: Option[CodeSerializer[CodeType]] = None
  
  lazy val scalaCompiler = new ScalaRuntimeCompiler()
  
  def createRuntime(options: OrcExecutionOptions): Runtime[CodeType] = new RunCompiledRuntime() with Runtime[CodeType] {
    def start(mod: CodeType, k: orc.OrcEvent => Unit): ExecutionHandle = {
      start(mod, k, options)
    }
    
    def run(code: CodeType, k: orc.OrcEvent => Unit): Unit = {
      start(code, k)
    }
    def runSynchronous(code: CodeType,k: orc.OrcEvent => Unit): Unit = {
      val h = start(code, k)
      h.waitForHalt()
    }
    def stop(): Unit = kill()
  }
}
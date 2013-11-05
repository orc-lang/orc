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
import orc.ast.porc.{Expr => AstExpr}
import orc.run.porc.{Expr => RunExpr}
import orc.compile.PorcOrcCompiler
import orc.run.porc.Interpreter
import orc.ast.porc.TranslateToPorcEval

/**
  *
  * @author amp
  */
class PorcBackend extends Backend[RunExpr] {
  lazy val compiler: Compiler[RunExpr] = new PorcOrcCompiler() with Compiler[RunExpr] {
    def compile(source: OrcInputContext, options: OrcCompilationOptions, 
        compileLogger: CompileLogger, progress: ProgressMonitor): RunExpr = 
      this(source, options, compileLogger, progress)
  }
  
  val serializer: Option[CodeSerializer[RunExpr]] = None
  
  def createRuntime(options: OrcExecutionOptions): Runtime[RunExpr] = new Interpreter() with Runtime[RunExpr] {
    def run(code: RunExpr,k: orc.OrcEvent => Unit): Unit = {
      start(code, k, options)      
    }
    def runSynchronous(code: RunExpr,k: orc.OrcEvent => Unit): Unit = {
      val h = start(code, k, options)  
      h.waitForHalt()
    }
    def stop(): Unit = kill()
  }
}
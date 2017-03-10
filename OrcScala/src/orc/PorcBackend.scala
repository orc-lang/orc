//
// PorcBackend.scala -- Scala class PorcBackend
// Project OrcScala
//
// Created by amp on Aug 28, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import java.io.{ IOException, OutputStreamWriter }
import orc.ast.orctimizer.named.Expression
import orc.compile.StandardOrcCompiler
import orc.compile.parse.OrcInputContext
import orc.error.compiletime.CompileLogger
import orc.error.loadtime.{ LoadingException, OilParsingException }
import orc.progress.ProgressMonitor
import orc.run.StandardOrcRuntime
import orc.compile.tojava.JavaCompiler
import orc.compile.orctimizer.PorcOrcCompiler
import orc.run.tojava.ToJavaRuntime
import orc.run.tojava.Execution
import orc.run.tojava.OrcProgram

/** A backend implementation using the Orctimizer and Porc compilers.
  *
  * @author amp
  */
class PorcBackend extends Backend[PorcBackend.CompiledOrcProgram] {
  import PorcBackend.CompiledOrcProgram

  lazy val compiler: Compiler[CompiledOrcProgram] = new PorcOrcCompiler() with Compiler[CompiledOrcProgram] {
    val javaCompiler = new JavaCompiler()
    def compile(source: OrcInputContext, options: OrcCompilationOptions,
      compileLogger: CompileLogger, progress: ProgressMonitor): CompiledOrcProgram = {
      val code = this(source, options, compileLogger, progress)
      if (code != null) {
        val cls = javaCompiler(code)
        cls
      } else {
        null
      }
    }
  }

  val serializer: Option[CodeSerializer[CompiledOrcProgram]] = None

  def createRuntime(options: OrcExecutionOptions): Runtime[CompiledOrcProgram] = new StandardOrcRuntime("To Java via Porc") with Runtime[CompiledOrcProgram] {
    val tjruntime = new ToJavaRuntime(this)
    startScheduler(options)

    private def start(cls: CompiledOrcProgram, k: orc.OrcEvent => Unit): Execution = {
      val prog = cls.newInstance()

      // TODO: Remove this weird type hack.
      prog.run(tjruntime, k.asInstanceOf[OrcEvent => scala.runtime.BoxedUnit])
    }

    def run(cls: CompiledOrcProgram, k: orc.OrcEvent => Unit): Unit = start(cls, k)
    def runSynchronous(cls: CompiledOrcProgram, k: orc.OrcEvent => Unit): Unit = start(cls, k).waitForHalt()
  }
}

object PorcBackend {
  type CompiledOrcProgram = Class[_ <: OrcProgram]
}

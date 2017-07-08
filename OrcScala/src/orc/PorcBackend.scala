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
import orc.compile.orctimizer.PorcOrcCompiler
import orc.ast.porc.MethodCPS

/** A backend implementation using the Orctimizer and Porc compilers.
  *
  * This is designed to be extended with a runtime which takes Porc as input.
  *
  * @author amp
  */
abstract class PorcBackend extends Backend[MethodCPS] {
  lazy val compiler: Compiler[MethodCPS] = new PorcOrcCompiler() with Compiler[MethodCPS] {
    def compile(source: OrcInputContext, options: OrcCompilationOptions,
      compileLogger: CompileLogger, progress: ProgressMonitor): MethodCPS = {
      this(source, options, compileLogger, progress)
    }
  }


  // NOTE: If needed we could implement an XML serializer for Porc. We could also make things even simpler by just using java serialization here.
  val serializer: Option[CodeSerializer[MethodCPS]] = None

  def createRuntime(options: OrcExecutionOptions): Runtime[MethodCPS]
}

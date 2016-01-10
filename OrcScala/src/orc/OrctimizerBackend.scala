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

import java.io.{ IOException, OutputStreamWriter }
import orc.ast.orctimizer.named.Expression
import orc.compile.StandardOrcCompiler
import orc.compile.parse.OrcInputContext
import orc.error.compiletime.CompileLogger
import orc.error.loadtime.{ LoadingException, OilParsingException }
import orc.progress.ProgressMonitor
import orc.run.StandardOrcRuntime
import orc.compile.orctimizer.OrctimizerOrcCompiler

/** A backend implementation using the Token interpreter.
  *
  * @author amp
  */
class OrctimizerBackend extends Backend[String] {
  lazy val compiler: Compiler[String] = new OrctimizerOrcCompiler() with Compiler[String] {
    def compile(source: OrcInputContext, options: OrcCompilationOptions,
      compileLogger: CompileLogger, progress: ProgressMonitor): String = this(source, options, compileLogger, progress)
  }

  val serializer: Option[CodeSerializer[String]] = None

  def createRuntime(options: OrcExecutionOptions): Runtime[String] = ???
}
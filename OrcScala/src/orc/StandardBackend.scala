//
// StandardBackend.scala -- Scala class/trait/object StandardBackend
// Project OrcScala
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

import orc.ast.oil.nameless.Expression
import orc.ast.oil.xml.OrcXML
import orc.compile.StandardOrcCompiler
import orc.compile.parse.OrcInputContext
import orc.error.compiletime.CompileLogger
import orc.error.loadtime.{ LoadingException, OilParsingException }
import orc.progress.ProgressMonitor
import orc.run.StandardOrcRuntime

/** A backend implementation using the Token interpreter.
  *
  * @author amp
  */
class StandardBackend extends Backend[Expression] {
  lazy val compiler: Compiler[Expression] = new StandardOrcCompiler() with Compiler[Expression] {
    def compile(source: OrcInputContext, options: OrcCompilationOptions,
      compileLogger: CompileLogger, progress: ProgressMonitor): Expression = this(source, options, compileLogger, progress)
  }

  val serializer: Option[CodeSerializer[Expression]] = Some(new CodeSerializer[Expression] {
    @throws(classOf[LoadingException])
    def deserialize(in: java.io.InputStream): orc.ast.oil.nameless.Expression = {
      OrcXML.readOilFromStream(in) match {
        case e: Expression => e
        case _ => throw new OilParsingException("Top-level element of input was not an Expression.")
      }
    }

    def serialize(code: orc.ast.oil.nameless.Expression, out: java.io.OutputStream): Unit = {
      new OutputStreamWriter(out).write(OrcXML.toXML(code).toString)
    }
  })

//FIXME:Revert this hack: orc.run.distrib.LeaderRuntime() c'tor should be StandardOrcRuntime("Orc")  
  def createRuntime(options: OrcExecutionOptions): Runtime[Expression] = new orc.run.distrib.LeaderRuntime() with Runtime[Expression] {
    def run(code: orc.ast.oil.nameless.Expression, k: orc.OrcEvent => Unit): Unit = run(code, k, options)
    def runSynchronous(code: orc.ast.oil.nameless.Expression, k: orc.OrcEvent => Unit): Unit = runSynchronous(code, k, options)
  }
}
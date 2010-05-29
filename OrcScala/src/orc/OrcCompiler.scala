//
// OrcCompiler.scala -- Scala class OrcCompiler
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import java.io.PrintWriter

import scala.util.parsing.input.Reader
import scala.util.parsing.input.StreamReader

import orc.error.compiletime.CompilationException
import orc.error.compiletime.CompileLogger
import orc.error.compiletime.CompileLogger.Severity
import orc.error.compiletime.ParsingException
import orc.error.compiletime.PrintWriterCompileLogger

//type CompilerPhase[O, T] = O => T => T

abstract class CompilerDriver[S, T, O](val parser: O => S => T, val phaseList: Traversable[O => T => T]) extends (O => S => T) {
  def apply(options: O)(source: S): T = {
    val composedPhases: T => T = phaseList map { _(options) } reduceLeft { (f, g) => g compose f }
    composedPhases(parser(options)(source))
  }
}

/**
 * An instance of OrcCompiler is a particular Orc compiler configuration, 
 * which is a particular Orc compiler implementation, in a JVM instance,
 * with possibly certain implementation-dependent parameters fixed.
 * Note, however, that an OrcCompiler instance is not specialized for
 * a single Orc program; in fact, multiple compilations of different programs,
 * with different options set, may be in progress concurrently within a
 * single OrcCompiler instance.  
 *
 * @author jthywiss
 */
class OrcCompiler extends OrcCompilerAPI {

  val translator = new orc.translation.Translator

  def apply(source: Reader[Char], options: OrcOptions): orc.oil.Expression = {
      try {
        compileLogger.beginProcessing(options.filename)
        val extendedAst = OrcParser.parse(options, source) match {
          case OrcParser.Success(result, _) => result 
          case OrcParser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos) ; null
        }
        val oilAst = translator.translate(options, extendedAst)
        val refinedAst = refineOil(oilAst)
        refinedAst
      } catch {case e: CompilationException =>
        compileLogger.recordMessage(Severity.FATAL, 0, e.getMessageOnly, e.pos, null, e)
        null
      } finally {
        compileLogger.endProcessing(options.filename)
      }
  }

  def apply(source: java.io.Reader, options: OrcOptions): orc.oil.Expression = apply(StreamReader(source), options)

  val compileLogger: CompileLogger = new PrintWriterCompileLogger(new PrintWriter(System.err, true))

}

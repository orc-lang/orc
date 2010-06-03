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

package orc.compile

import java.io.InputStreamReader
import java.io.PrintWriter

import scala.util.parsing.input.Reader
import scala.util.parsing.input.StreamReader

import orc.error.compiletime.CompilationException
import orc.error.compiletime.CompileLogger
import orc.error.compiletime.CompileLogger.Severity
import orc.error.compiletime.ParsingException
import orc.error.compiletime.PrintWriterCompileLogger
import orc.OrcCompilerAPI
import orc.OrcOptions
import orc.compile.parse.OrcParser

/**
 * Represents one phase in a compiler.  It is defined as a function from
 * compiler options to a function from a phase's input to its output.
 * CompilerPhases can be composed with the >>> operator.
 *
 * @author jthywiss
 */
abstract trait CompilerPhase[O, A, B] extends (O => A => B) { self =>
  val phaseName: String
  def >>>[C](that: CompilerPhase[O, B, C]) = new CompilerPhase[O, A, C] { 
    val phaseName = self.phaseName+" >>> "+that.phaseName
    override def apply(o: O) = { a: A => that(o)(self.apply(o)(a)) }
  }
}

/**
 * An instance of OrcCompiler is a particular Orc compiler configuration, 
 * which is a particular Orc compiler implementation, in a JVM instance.
 * Note, however, that an OrcCompiler instance is not specialized for
 * a single Orc program; in fact, multiple compilations of different programs,
 * with different options set, may be in progress concurrently within a
 * single OrcCompiler instance.  
 *
 * @author jthywiss
 */
class OrcCompiler extends OrcCompilerAPI {
  //TODO: Skip remaining phases when compileLogger.getMaxSeverity >= FATAL 

  val parse = new CompilerPhase[OrcOptions, Reader[Char], orc.compile.ext.Expression] {
    val phaseName = "parse"
    override def apply(options: OrcOptions) = { source =>
      if (!options.noPrelude) {
        //FIXME: Hack for testing -- only reading prelude/core.inc
        val preludeReader = StreamReader(new InputStreamReader(getClass().getResourceAsStream("/orc/lib/includes/prelude/core.inc")))
        val preludeParseResult = OrcParser.parseInclude(options, preludeReader, "prelude/core.inc")
        preludeParseResult match {
          case OrcParser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos)
          case _ => {}
        }
        OrcParser.parse(options, source) match {
          case OrcParser.Success(result, _) => ext.Declare(preludeParseResult.get,result)
          case OrcParser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos)
        }
      } else {
        OrcParser.parse(options, source) match {
          case OrcParser.Success(result, _) => result
          case OrcParser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos)
        }
      }
    }
  }

  val translate = new CompilerPhase[OrcOptions, orc.compile.ext.Expression, orc.oil.named.Expression] { 
    val phaseName = "translate"
    override def apply(options: OrcOptions) = { ast =>
      orc.compile.translate.Translator.translate(options, ast)
    }
  }

  val typeCheck = new CompilerPhase[OrcOptions, orc.oil.named.Expression, orc.oil.named.Expression] {
    val phaseName = "typeCheck"
    override def apply(options: OrcOptions) = { ast => ast }
  }

  val refineNamedOil = new CompilerPhase[OrcOptions, orc.oil.named.Expression, orc.oil.named.Expression] {
    val phaseName = "refineNamedOil"
    override def apply(options: OrcOptions) = { ast => ast }
  }
  
  val deBruijn = new CompilerPhase[OrcOptions, orc.oil.named.Expression, orc.oil.nameless.Expression] {
    val phaseName = "deBruijn"
    override def apply(options: OrcOptions) = { ast => ast.withoutNames }
  }

  val phases = parse >>> translate >>> typeCheck >>> refineNamedOil >>> deBruijn

  def apply(source: Reader[Char], options: OrcOptions): orc.oil.nameless.Expression = {
    compileLogger.beginProcessing(options.filename)
    try {
      phases(options)(source)
    } catch {case e: CompilationException =>
      compileLogger.recordMessage(Severity.FATAL, 0, e.getMessageOnly, e.pos, null, e)
      null
    } finally {
      compileLogger.endProcessing(options.filename)
    }
  }

  def apply(source: java.io.Reader, options: OrcOptions): orc.oil.nameless.Expression = apply(StreamReader(source), options)

  val compileLogger: CompileLogger = new PrintWriterCompileLogger(new PrintWriter(System.err, true))

}

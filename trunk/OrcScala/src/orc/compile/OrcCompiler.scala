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
import scala.collection.JavaConversions._

import orc.error.compiletime.CompilationException
import orc.error.compiletime.CompileLogger
import orc.error.compiletime.CompileLogger.Severity
import orc.error.compiletime.ParsingException
import orc.error.compiletime.PrintWriterCompileLogger
import orc.OrcCompilerAPI
import orc.CompilerEnvironmentIfc
import orc.OrcOptions
import orc.compile.parse.OrcParser

/**
 * Represents one phase in a compiler.  It is defined as a function from
 * compiler options to a function from a phase's input to its output.
 * CompilerPhases can be composed with the >>> operator.
 *
 * @author jthywiss
 */
trait CompilerPhase[O, A, B] extends (O => A => B) { self =>
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
class OrcCompiler extends OrcCompilerAPI with CompilerEnvironmentIfc {
  //TODO: Skip remaining phases when compileLogger.getMaxSeverity >= FATAL 

  val parse = new CompilerPhase[OrcOptions, Reader[Char], orc.compile.ext.Expression] {
    val phaseName = "parse"
    override def apply(options: OrcOptions) = { source =>
      var includeFileNames = options.additionalIncludes
      if (options.usePrelude) {
        //FIXME: Hack for testing -- only reading prelude/core.inc
        includeFileNames = "prelude/core.inc" :: (includeFileNames).toList
      }
      val includeAsts = for (fileName <- includeFileNames) yield {
        val preludeReader = StreamReader(openInclude(fileName, null, options))
        val preludeParseResult = OrcParser.parseInclude(options, preludeReader, fileName)
        preludeParseResult match {
          case OrcParser.Success(result, _) => preludeParseResult.get
          case OrcParser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos)
        }
      }
      println(includeAsts)
      val progAst = OrcParser.parse(options, source) match {
        case OrcParser.Success(result, _) => result
        case OrcParser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos)
      }
      (includeAsts :\ progAst) { orc.compile.ext.Declare }
    }
  }

  val translate = new CompilerPhase[OrcOptions, orc.compile.ext.Expression, orc.oil.named.Expression] { 
    val phaseName = "translate"
    override def apply(options: OrcOptions) = { ast =>
      val result = orc.compile.translate.Translator.translate(options, ast)
      //println(result)
      result
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

  //TODO: Parameterize on these methods:
  val compileLogger: CompileLogger = new PrintWriterCompileLogger(new PrintWriter(System.err, true))

  def openInclude(includeFileName: String, relativeToFileName: String, options: OrcOptions): java.io.Reader = {
    
    // Try filename under the include path list
    for (ip <- options.includePath) {
      val incPath = new java.io.File(ip);

      /* Build file path as follows:
       *   path = relTo + incPath + fileName
       * If relTo is null, incPath must be absolute (or the path entry is skipped)
       * Try for all paths in the include path list
       */
      if (incPath.isAbsolute() || relativeToFileName != null) {
        val incPathPrefixed = new java.io.File(relativeToFileName, ip);
        val file = new java.io.File(incPathPrefixed, includeFileName);
        if (file.exists()) {
          return new java.io.FileReader(file);
        }
      }
    }
    
    // Try in the bundled include resources
    val stream = options.getClass().getResourceAsStream("/orc/lib/includes/" + includeFileName);
    if (stream != null) {
      return new java.io.InputStreamReader(stream);
    }

    // Try to read this include as a URL instead of as a local file
    try {
      val incurl = new java.net.URL(includeFileName);
      return new InputStreamReader(incurl.openConnection().getInputStream());
    } catch {
      case e: java.net.MalformedURLException => { } //ignore
      case e: java.io.IOException =>
        throw new java.io.FileNotFoundException("Could not open a connection to '" + includeFileName + "'.");
    }

    throw new java.io.FileNotFoundException("Include file '" + includeFileName + "' not found; check the include path.");
  }

}

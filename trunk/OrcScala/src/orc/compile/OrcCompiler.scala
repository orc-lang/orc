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
import scala.compat.Platform.currentTime

import orc.error.compiletime.CompilationException
import orc.error.compiletime.CompileLogger
import orc.error.compiletime.CompileLogger.Severity
import orc.error.compiletime.ParsingException
import orc.error.compiletime.PrintWriterCompileLogger
import orc.OrcOptions
import orc.compile.parse.OrcParser
import orc.compile.parse.OrcReader
import orc.OrcCompiler

/**
 * Represents one phase in a compiler.  It is defined as a function from
 * compiler options to a function from a phase's input to its output.
 * CompilerPhases can be composed with the >>> operator.
 *
 * @author jthywiss
 */
trait CompilerPhase[O, A, B] extends (O => A => B) { self =>
  val phaseName: String
  def >>>[C >: Null](that: CompilerPhase[O, B, C]) = new CompilerPhase[O, A, C] { 
    val phaseName = self.phaseName+" >>> "+that.phaseName
    override def apply(o: O) = { a: A => 
      if (a == null) null else {
        val b = self.apply(o)(a)
        if (b == null) null else {
          that(o)(b)
        }
      }
    }
  }
  def timePhase: CompilerPhase[O, A, B] = new CompilerPhase[O, A, B] { 
    val phaseName = self.phaseName
    override def apply(o: O) = { a: A =>
      val phaseStart = currentTime
      val b = self.apply(o)(a)
      val phaseEnd = currentTime
      Console.err.println("[phase duration: "+phaseName+": "+(phaseEnd-phaseStart)+" ms]")
      b
    }
  }
  def printOut: CompilerPhase[O, A, B] = new CompilerPhase[O, A, B] { 
    val phaseName = self.phaseName
    override def apply(o: O) = { a: A =>
      val b = self.apply(o)(a)
      Console.err.println(phaseName+" result = "+b.toString())
      b
    }
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
class StandardOrcCompiler extends OrcCompiler {
  
  val parse = new CompilerPhase[OrcOptions, Reader[Char], orc.compile.ext.Expression] {
    val phaseName = "parse"
    override def apply(options: OrcOptions) = { source =>
      var includeFileNames = options.additionalIncludes
      val parser = new OrcParser(options)
      if (options.usePrelude) {
        includeFileNames = "prelude.inc" :: (includeFileNames).toList
      }
      val includeAsts = for (fileName <- includeFileNames) yield {
        val r = OrcReader(openInclude(fileName, null, options), fileName, openInclude(_, _, options))
        parser.scanAndParseInclude(r, fileName) match {
          case parser.Success(result, _) => result
          case parser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos)
        }
      }
      val progAst = parser.scanAndParseProgram(source) match {
        case parser.Success(result, _) => result
        case parser.NoSuccess(msg, in) => throw new ParsingException(msg, in.pos)
      }
      (includeAsts :\ progAst) { orc.compile.ext.Declare }
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
    override def apply(options: OrcOptions) = { ast => ast.withoutUnusedDefs.withoutUnusedTypes }
  }
  
  val deBruijn = new CompilerPhase[OrcOptions, orc.oil.named.Expression, orc.oil.nameless.Expression] {
    val phaseName = "deBruijn"
    override def apply(options: OrcOptions) = { ast => ast.withoutNames }
  }

  
  
  /* ***************** */
  /*                   */
  /*  Compiler Phases  */
  /*                   */
  /* ***************** */
  
  val phases = parse.timePhase >>> translate.timePhase >>> typeCheck.timePhase >>> refineNamedOil.timePhase.printOut >>> deBruijn.timePhase

  
  
  
  
  def apply(source: Reader[Char], options: OrcOptions): orc.oil.nameless.Expression = {
    compileLogger.beginProcessing(options.filename)
    try {
      phases(options)(source)
    } catch {case e: CompilationException =>
      compileLogger.recordMessage(Severity.FATAL, 0, e.getMessageOnly, e.getPosition(), null, e)
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
      val incPath = new java.io.File(ip)

      /* Build file path as follows:
       *   path = relTo + incPath + fileName
       * If relTo is null, incPath must be absolute (or the path entry is skipped)
       * Try for all paths in the include path list
       */
      if (incPath.isAbsolute() || relativeToFileName != null) {
        val incPathPrefixed = new java.io.File(relativeToFileName, ip)
        val file = new java.io.File(incPathPrefixed, includeFileName)
        if (file.exists()) {
          return new java.io.FileReader(file)
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

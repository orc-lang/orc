//
// OrcCompiler.scala -- Scala classes CoreOrcCompiler and StandradOrcCompiler
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

import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.Writer
import java.io.PrintWriter
import java.io.IOException
import java.io.FileNotFoundException
import java.net.URI

import scala.util.parsing.input.Reader
import scala.util.parsing.input.StreamReader
import scala.collection.JavaConversions._
import scala.compat.Platform.currentTime

import orc.OrcCompiler
import orc.OrcOptions
import orc.compile.parse.OrcIncludeParser
import orc.compile.parse.OrcProgramParser
import orc.compile.parse.OrcInputContext
import orc.compile.parse.OrcResourceInputContext
import orc.compile.optimize._
import orc.error.compiletime.CompilationException
import orc.error.compiletime.CompileLogger
import orc.error.compiletime.CompileLogger.Severity
import orc.error.compiletime.ParsingException
import orc.error.compiletime.PrintWriterCompileLogger
import orc.values.sites.SiteClassLoading


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
 * An instance of CoreOrcCompiler is a particular Orc compiler configuration, 
 * which is a particular Orc compiler implementation, in a JVM instance.
 * Note, however, that an CoreOrcCompiler instance is not specialized for
 * a single Orc program; in fact, multiple compilations of different programs,
 * with different options set, may be in progress concurrently within a
 * single CoreOrcCompiler instance.  
 *
 * @author jthywiss
 */
abstract class CoreOrcCompiler extends OrcCompiler {
  
  ////////
  // Definition of the phases of the compiler
  ////////

  val parse = new CompilerPhase[OrcOptions, OrcInputContext, orc.compile.ext.Expression] {
    val phaseName = "parse"
    override def apply(options: OrcOptions) = { source =>
      var includeFileNames = options.additionalIncludes
      if (options.usePrelude) {
        includeFileNames = "prelude.inc" :: (includeFileNames).toList
      }
      val includeAsts = for (fileName <- includeFileNames) yield {
        val ic = openInclude(fileName, null, options)
        OrcIncludeParser(ic, options, CoreOrcCompiler.this) match {
          case r: OrcIncludeParser.SuccessT[_] => r.get.asInstanceOf[OrcIncludeParser.ResultType]
          case n: OrcIncludeParser.NoSuccess   => throw new ParsingException(n.msg, n.next.pos)
        }
      }
      val progAst = OrcProgramParser(source, options, CoreOrcCompiler.this) match {
          case r: OrcProgramParser.SuccessT[_] => r.get.asInstanceOf[OrcProgramParser.ResultType]
          case n: OrcProgramParser.NoSuccess   => throw new ParsingException(n.msg, n.next.pos)
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
    override def apply(options: OrcOptions) =
      (e : orc.oil.named.Expression) => { 
        val refine = FractionDefs andThen RemoveUnusedDefs andThen RemoveUnusedTypes
        refine(e)
      }
  }
  
  val deBruijn = new CompilerPhase[OrcOptions, orc.oil.named.Expression, orc.oil.nameless.Expression] {
    val phaseName = "deBruijn"
    override def apply(options: OrcOptions) = { ast => ast.withoutNames }
  }

  ////////
  // Compose phases into a compiler
  ////////

  val phases = parse.timePhase >>> translate.timePhase >>> typeCheck.timePhase >>> refineNamedOil.timePhase >>> deBruijn.timePhase

  ////////
  // Compiler methods
  ////////

  def apply(source: OrcInputContext, options: OrcOptions, compileLogger: CompileLogger): orc.oil.nameless.Expression = {
    compileLogger.beginProcessing(options.filename)
    try {
      val result = phases(options)(source)
      if (compileLogger.getMaxSeverity().ordinal() >= Severity.ERROR.ordinal()) null else result
    } catch {case e: CompilationException =>
      compileLogger.recordMessage(Severity.FATAL, 0, e.getMessage, e.getPosition(), null, e)
      null
    } finally {
      compileLogger.endProcessing(options.filename)
    }
  }

}


/**
 * StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces. 
 *
 * @author jthywiss
 */
class StandardOrcCompiler() extends CoreOrcCompiler with SiteClassLoading {
  override def apply(source: OrcInputContext, options: OrcOptions, compileLogger: CompileLogger): orc.oil.nameless.Expression = {
    SiteClassLoading.initWithClassPathStrings(options.classPath)
    super.apply(source, options, compileLogger)
  }

  private class OrcReaderInputContext(val javaReader: java.io.Reader, override val descr: String) extends OrcInputContext {
    val file = new File(descr)
    override val reader = orc.compile.parse.OrcReader(new BufferedReader(javaReader), descr)
    override def toURI = file.toURI
    override def toURL = toURI.toURL
  }

  def apply(source: java.io.Reader, options: OrcOptions, err: Writer): orc.oil.nameless.Expression = {
    this(new OrcReaderInputContext(source, options.filename), options, new PrintWriterCompileLogger(new PrintWriter(err, true)))
  }

  private object OrcNullInputContext extends OrcInputContext {
    override val descr = ""
    override val reader = null
    override val toURI = new URI("")
    override def toURL = toURI.toURL
  }

  def openInclude(includeFileName: String, relativeTo: OrcInputContext, options: OrcOptions): OrcInputContext = {
    val baseIC = if (relativeTo != null) relativeTo else OrcNullInputContext

    // Try filename under the include path list
    for (incPath <- scala.collection.JavaConversions.asIterable(options.includePath)) {
      try {
        //FIXME: Security implications of including local files:
        // For Orchard's sake, OrcJava disallowed relative file names
        // in certain cases, to prevent examining files by including
        // them.  This seems a weak barrier, and in fact was broken.
        // We need an alternative way to control local file reads.
        return baseIC.newInputFromPath(incPath, includeFileName)
      } catch {
        case _: IOException => /* Ignore, must not be here */
      }
    }
    
    // Try in the bundled include resources
    try {
      return new OrcResourceInputContext("orc/lib/includes/" + includeFileName, getResource)
    } catch {
      case _: IOException => /* Ignore, must not be here */
    }

    throw new FileNotFoundException("Include file '" + includeFileName + "' not found; check the include path.");
  }
}

//
// OrcCompiler.scala -- Scala classes CoreOrcCompiler and StandardOrcCompiler
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 26, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile

import java.io.{ BufferedReader, File, FileNotFoundException, IOException, PrintWriter, Writer, FileOutputStream }
import java.net.{ MalformedURLException, URI, URISyntaxException }
import orc.{ OrcCompilationOptions, OrcCompiler }
import orc.compile.optimize._
import orc.compile.parse.{ OrcNetInputContext, OrcResourceInputContext, OrcInputContext, OrcProgramParser, OrcIncludeParser }
import orc.compile.translate.Translator
import orc.compile.typecheck.Typechecker
import orc.error.compiletime._
import orc.error.compiletime.CompileLogger.Severity
import orc.error.OrcExceptionExtension._
import orc.progress.{ NullProgressMonitor, ProgressMonitor }
import orc.values.sites.SiteClassLoading
import scala.collection.JavaConversions._
import scala.compat.Platform.currentTime
import orc.ast.oil.xml.OrcXML
import orc.compile.translate.TranslateVclock
import orc.OrcCompilerRequires
import orc.ast.ext
import orc.ast.oil4c.{ named => named4c }
import orc.ast.oil.{ named => named5c }
import orc.compile.translate.SplitPrune

/** Represents a configuration state for a compiler.
  */
class CompilerOptions(val options: OrcCompilationOptions, val compileLogger: CompileLogger) {

  def reportProblem(exn: CompilationException with ContinuableSeverity) {
    compileLogger.recordMessage(exn.severity, 0, exn.getMessage(), exn.getPosition(), exn)
  }

}

/** Represents one phase in a compiler.  It is defined as a function from
  * compiler options to a function from a phase's input to its output.
  * CompilerPhases can be composed with the >>> operator.
  *
  * @author jthywiss
  */
trait CompilerPhase[O, A, B] extends (O => A => B) { self =>
  val phaseName: String
  def >>>[C >: Null](that: CompilerPhase[O, B, C]) = new CompilerPhase[O, A, C] {
    val phaseName = self.phaseName + " >>> " + that.phaseName
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
      Logger.fine("phase duration: " + phaseName + ": " + (phaseEnd - phaseStart) + " ms")
      b
    }
  }
  def printOut: CompilerPhase[O, A, B] = new CompilerPhase[O, A, B] {
    val phaseName = self.phaseName
    override def apply(o: O) = { a: A =>
      val b = self.apply(o)(a)
      Logger.info(phaseName + " result = " + b.toString())
      b
    }
  }
}

/** A mix-in that provides the phases used by the standard compiler. They are in a
  * mix-in so that they can be used in other compilers that do not use the same output
  * type as the standard orc compiler.
  */
trait CoreOrcCompilerPhases {
  this: OrcCompilerRequires =>

  ////////
  // Definition of the phases of the compiler
  ////////

  val parse = new CompilerPhase[CompilerOptions, OrcInputContext, orc.ast.ext.Expression] {
    val phaseName = "parse"
    @throws(classOf[IOException])
    override def apply(co: CompilerOptions) = { source =>
      val topLevelSourcePos = source.reader.pos
      var includeFileNames = co.options.additionalIncludes
      if (co.options.usePrelude) {
        includeFileNames = "prelude.inc" :: (includeFileNames).toList
      }
      val includeAsts = for (fileName <- includeFileNames) yield {
        val ic = openInclude(fileName, null, co.options)
        co.compileLogger.beginDependency(ic);
        try {
          OrcIncludeParser(ic, co, CoreOrcCompilerPhases.this) match {
            case r: OrcIncludeParser.SuccessT[_] => r.get.asInstanceOf[OrcIncludeParser.ResultType]
            case n: OrcIncludeParser.NoSuccess => throw new ParsingException(n.msg, n.next.pos)
          }
        } finally {
          co.compileLogger.endDependency(ic);
        }
      }
      val progAst = OrcProgramParser(source, co, CoreOrcCompilerPhases.this) match {
        case r: OrcProgramParser.SuccessT[_] => r.get.asInstanceOf[OrcProgramParser.ResultType]
        case n: OrcProgramParser.NoSuccess => throw new ParsingException(n.msg, n.next.pos)
      }
      (includeAsts :\ progAst) { orc.ast.ext.Declare } setPos topLevelSourcePos
    }
  }

  val translate = new CompilerPhase[CompilerOptions, ext.Expression, named4c.Expression] {
    val phaseName = "translate"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new Translator(co.reportProblem)
        translator.translate(ast)
      }
  }

  val vClockTrans = new CompilerPhase[CompilerOptions, named4c.Expression, named4c.Expression] {
    val phaseName = "vClockTrans"
    override def apply(co: CompilerOptions) = new TranslateVclock(co.reportProblem)(_)
  }

  val noUnboundVars = new CompilerPhase[CompilerOptions, named4c.Expression, named4c.Expression] {
    val phaseName = "noUnboundVars"
    override def apply(co: CompilerOptions) = { ast =>
      for (x <- ast.unboundvars) {
        co.reportProblem(UnboundVariableException(x.name) at x)
      }
      for (u <- ast.unboundtypevars) {
        co.reportProblem(UnboundTypeVariableException(u.name) at u)
      }
      ast
    }
  }

  val fractionDefs = new CompilerPhase[CompilerOptions, named4c.Expression, named4c.Expression] {
    val phaseName = "fractionDefs"
    override def apply(co: CompilerOptions) = { FractionDefs(_) }
  }

  val removeUnusedDefs = new CompilerPhase[CompilerOptions, named5c.Expression, named5c.Expression] {
    val phaseName = "removeUnusedDefs"
    override def apply(co: CompilerOptions) = { ast => RemoveUnusedDefs(ast) }
  }

  val removeUnusedTypes = new CompilerPhase[CompilerOptions, named5c.Expression, named5c.Expression] {
    val phaseName = "removeUnusedTypes"
    override def apply(co: CompilerOptions) = { ast => RemoveUnusedTypes(ast) }
  }

  val typeCheck = new CompilerPhase[CompilerOptions, named4c.Expression, named4c.Expression] {
    val phaseName = "typeCheck"
    override def apply(co: CompilerOptions) = { ast =>
      if (co.options.typecheck) {
        val typechecker = new Typechecker(co.reportProblem)
        val (newAst, programType) = typechecker.typecheck(ast)
        val typeReport = "Program type checks as " + programType.toString
        co.compileLogger.recordMessage(CompileLogger.Severity.INFO, 0, typeReport, newAst.pos, newAst)
        newAst
      } else {
        ast
      }
    }
  }

  val noUnguardedRecursion = new CompilerPhase[CompilerOptions, named4c.Expression, named4c.Expression] {
    val phaseName = "noUnguardedRecursion"
    override def apply(co: CompilerOptions) =
      { ast =>
        def warn(e: named4c.Expression) = {
          co.reportProblem(UnguardedRecursionException() at e)
        }
        if (!co.options.disableRecursionCheck) {
          ast.checkGuarded(warn)
        }
        ast
      }
  }

  val splitPrune = new CompilerPhase[CompilerOptions, named4c.Expression, named5c.Expression] {
    val phaseName = "splitPrune"
    override def apply(co: CompilerOptions) = { ast =>
      val translator = new SplitPrune(co.reportProblem)
      translator(ast)
    }
  }

  val deBruijn = new CompilerPhase[CompilerOptions, named5c.Expression, orc.ast.oil.nameless.Expression] {
    val phaseName = "deBruijn"
    override def apply(co: CompilerOptions) = { ast => ast.withoutNames }
  }

  def outputIR[A](irNumber: Int) = new CompilerPhase[CompilerOptions, A, A] {
    val phaseName = s"Output IR #$irNumber"
    override def apply(co: CompilerOptions) = { ast =>
      val irMask = 1 << (irNumber-1)
      val echoIR = co.options.echoIR
      if ((echoIR & irMask) == irMask) {
        println(s"============ Begin Dump IR #$irNumber with type ${ast.getClass.getCanonicalName} ============")
        println(ast)
        println(s"============ End dump IR #$irNumber with type ${ast.getClass.getCanonicalName} ============")
      }

      ast
    }
  }

  // Generate XML for the AST and echo it to console; useful for testing.
  val outputOil = new CompilerPhase[CompilerOptions, orc.ast.oil.nameless.Expression, orc.ast.oil.nameless.Expression] {
    val phaseName = "outputOil"
    override def apply(co: CompilerOptions) = { ast =>

      if (co.options.echoOil) {
        val xml = OrcXML.astToXml(ast)
        val xmlnice = {
          val pp = new scala.xml.PrettyPrinter(80, 2)
          val xmlheader = """<?xml version="1.0" encoding="UTF-8" ?>""" + "\n"
          xmlheader + pp.format(xml)
        }
        println("Echoing OIL to console.")
        println("Caution: Echo on console will not accurately reproduce whitespace in string constants.")
        println()
        println(xmlnice)
      }

      co.options.oilOutputFile match {
        case Some(f) => {
          OrcXML.writeOilToStream(ast, new FileOutputStream(f))
        }
        case None => {}
      }

      ast
    }
  }
}

/** An instance of PhasedOrcCompiler is a particular Orc compiler configuration,
  * which is a particular Orc compiler implementation, in a JVM instance.
  * Note, however, that an CoreOrcCompiler instance is not specialized for
  * a single Orc program; in fact, multiple compilations of different programs,
  * with different options set, may be in progress concurrently within a
  * single CoreOrcCompiler instance.
  *
  * @author jthywiss, amp
  */
abstract class PhasedOrcCompiler[E >: Null] extends OrcCompiler[E] {
  val phases: CompilerPhase[CompilerOptions, OrcInputContext, E]

  ////////
  // Compiler methods
  ////////

  @throws(classOf[IOException])
  def apply(source: OrcInputContext, options: OrcCompilationOptions, compileLogger: CompileLogger, progress: ProgressMonitor): E = {
    //Logger.config(options)
    Logger.config("Begin compile " + options.filename)
    compileLogger.beginProcessing(source)
    try {
      val result = phases(new CompilerOptions(options, compileLogger))(source)
      if (compileLogger.getMaxSeverity().ordinal() >= Severity.ERROR.ordinal()) null else result
    } catch {
      case e: CompilationException =>
        compileLogger.recordMessage(Severity.FATAL, 0, e.getMessage, e.getPosition(), null, e)
        null
    } finally {
      compileLogger.endProcessing(source)
      Logger.config("End compile " + options.filename)
    }
  }

}

/** StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces
  * and specifies that compilation will finish with named.
  *
  * @author jthywiss
  */
class StandardOrcCompiler() extends PhasedOrcCompiler[orc.ast.oil.nameless.Expression]
  with StandardOrcCompilerEnvInterface[orc.ast.oil.nameless.Expression]
  with CoreOrcCompilerPhases {
  ////////
  // Compose phases into a compiler
  ////////

  val phases =
    parse.timePhase >>>
      translate.timePhase >>>
      vClockTrans.timePhase >>>
      noUnboundVars.timePhase >>>
      fractionDefs.timePhase >>>
      typeCheck.timePhase >>>
      noUnguardedRecursion.timePhase >>>
      outputIR(1) >>>
      splitPrune.timePhase >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      outputIR(2) >>>
      deBruijn.timePhase >>>
      outputOil
}

/** A mix-in for OrcCompiler that provides "standard" environment interfaces (via classloaders
  * and the file system).
  */
trait StandardOrcCompilerEnvInterface[+E] extends OrcCompiler[E] with SiteClassLoading {
  @throws(classOf[IOException])
  abstract override def apply(source: OrcInputContext, options: OrcCompilationOptions, compileLogger: CompileLogger, progress: ProgressMonitor): E = {
    // FIXME: This will break cases where the compiler is used to compile code that needs different class paths. 
    SiteClassLoading.initWithClassPathStrings(options.classPath)
    super.apply(source, options, compileLogger, progress) // This will call apply in the subCLASS of OrcCompiler
  }

  private class OrcReaderInputContext(val javaReader: java.io.Reader, override val descr: String) extends OrcInputContext {
    val file = new File(descr)
    override val reader = orc.compile.parse.OrcReader(new BufferedReader(javaReader), descr)
    override def toURI = file.toURI
    override def toURL = toURI.toURL
  }

  @throws(classOf[IOException])
  def apply(source: java.io.Reader, options: OrcCompilationOptions, err: Writer): E = {
    this(new OrcReaderInputContext(source, options.filename), options, new PrintWriterCompileLogger(new PrintWriter(err, true)), NullProgressMonitor)
  }

  private object OrcNullInputContext extends OrcInputContext {
    override val descr = ""
    override val reader = null
    override val toURI = new URI("")
    override def toURL = throw new UnsupportedOperationException("OrcNullInputContext.toURL")
  }

  @throws(classOf[IOException])
  def openInclude(includeFileName: String, relativeTo: OrcInputContext, options: OrcCompilationOptions): OrcInputContext = {
    val baseIC = if (relativeTo != null) relativeTo else OrcNullInputContext
    Logger.finer("openInclude " + includeFileName + ", relative to " + Option(baseIC.getClass.getCanonicalName).getOrElse(baseIC.getClass.getName) + "(" + baseIC.descr + ")")

    // If no include path, allow absolute HTTP and HTTPS includes
    if (options.includePath.isEmpty && (includeFileName.toLowerCase.startsWith("http://") || includeFileName.toLowerCase.startsWith("https://"))) {
      try {
        val newIC = new OrcNetInputContext(new URI(includeFileName))
        Logger.finer("include " + includeFileName + " opened as " + Option(newIC.getClass.getCanonicalName).getOrElse(newIC.getClass.getName) + "(" + newIC.descr + ")")
        return newIC
      } catch {
        case e: URISyntaxException => throw new FileNotFoundException("Include file '" + includeFileName + "' not found; Check URI syntax (" + e.getMessage + ")");
        case e: MalformedURLException => throw new FileNotFoundException("Include file '" + includeFileName + "' not found; Check URI syntax (" + e.getMessage + ")");
        case e: IOException => throw new FileNotFoundException("Include file '" + includeFileName + "' not found; IO error (" + e.getMessage + ")");
      }
    }

    // Try filename under the include path list
    for (incPath <- scala.collection.JavaConversions.collectionAsScalaIterable(options.includePath)) {
      try {
        //FIXME: Security implications of including local files:
        // For Orchard's sake, OrcJava disallowed relative file names
        // in certain cases, to prevent examining files by including
        // them.  This seems a weak barrier, and in fact was broken.
        // We need an alternative way to control local file reads.
        val newIC = baseIC.newInputFromPath(incPath, includeFileName)
        Logger.finer("include " + includeFileName + ", found on include path entry " + incPath + ", opened as " + Option(newIC.getClass.getCanonicalName).getOrElse(newIC.getClass.getName) + "(" + newIC.descr + ")")
        return newIC
      } catch {
        case _: IOException => /* Ignore, must not be here */
      }
    }

    // Try in the bundled include resources
    try {
      val newIC = new OrcResourceInputContext("orc/lib/includes/" + includeFileName, getResource)
      Logger.finer("include " + includeFileName + ", found in bundled resources, opened as " + Option(newIC.getClass.getCanonicalName).getOrElse(newIC.getClass.getName) + "(" + newIC.descr + ")")
      return newIC
    } catch {
      case _: IOException => /* Ignore, must not be here */
    }

    Logger.finer("include " + includeFileName + " not found")
    throw new FileNotFoundException("Include file '" + includeFileName + "' not found; check the include path.");
  }
}

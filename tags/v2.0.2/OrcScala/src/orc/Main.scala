//
// Main.scala -- Scala object Main
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 20, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import javax.script.{ScriptException, Compilable, ScriptEngine, ScriptEngineManager}
import javax.script.ScriptContext.ENGINE_SCOPE
import java.io.{PrintStream, FileNotFoundException, InputStreamReader, FileInputStream, File}
import scala.collection.JavaConversions._
import orc.error.OrcException
import orc.error.runtime.JavaException
import orc.script.{OrcBindings, OrcScriptEngine}
import orc.values.Format
import orc.util.CmdLineParser
import orc.util.CmdLineUsageException
import orc.util.PrintVersionAndMessageException
import orc.run.OrcDesktopEventAction
import orc.ast.oil.xml.OrcXML

/**
 * A command-line tool invocation of the Orc compiler and runtime engine
 *
 * @author jthywiss
 */
object Main {

  def main(args: Array[String]) {
    class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions
    try {
      val options = new OrcCmdLineOptions()
      options.parseCmdLine(args)
      setupLogging(options)
      
      val engine = (new ScriptEngineManager).getEngineByName("orc").asInstanceOf[ScriptEngine with Compilable]
      if (engine == null) throw new ClassNotFoundException("Unable to load Orc ScriptEngine")
      engine.setBindings(options, ENGINE_SCOPE)
      
      val stream = new FileInputStream(options.filename)
      
      val compiledOrc = 
        if (options.runOil) {
          /* Read precompiled OIL */
          val ast = OrcXML.readOilFromStream(stream)
          engine.asInstanceOf[OrcScriptEngine].loadDirectly(ast)
        }
        else {
          /* Read and compile Orc source */
          val reader = new InputStreamReader(stream, "UTF-8")
          engine.compile(reader).asInstanceOf[OrcScriptEngine#OrcCompiledScript]
        }
      
      if (options.compileOnly) { 
        if (options.runOil) { 
          Console.err.println("Warning: run-oil ignored since compile-only was also set.") 
        }
        return 
      }
      
      val printPubs = new OrcDesktopEventAction() {
        override def published(value: AnyRef) { println(Format.formatValue(value)); Console.out.flush() }
        override def caught(e: Throwable) { Console.out.flush(); printException(e, Console.err, options.showJavaStackTrace); Console.err.flush() }
      }
      compiledOrc.run(printPubs)
      
    } catch {
      case e: CmdLineUsageException => Console.err.println("Orc: " + e.getMessage)
      case e: PrintVersionAndMessageException => println(orcImplName+" "+orcVersion+"\n"+orcURL+"\n"+orcCopyright+"\n\n"+e.getMessage)
      case e: FileNotFoundException => Console.err.println("Orc: File not found: " + e.getMessage)
      case e: ScriptException if (e.getCause == null) => Console.err.println(e.getMessage)
      case e: ScriptException => printException(e.getCause, Console.err, false)
    }
  }

  val versionProperties = {
    val p = new java.util.Properties()
    val vp = getClass().getResourceAsStream("version.properties")
    if (vp == null) throw new java.util.MissingResourceException("Unable to load version.properties resource", "/orc/version.properties", "")
    p.load(vp)
    p
  }
  lazy val orcImplName: String = versionProperties.getProperty("orc.title")
  lazy val svnRevision: String = versionProperties.getProperty("orc.svn-revision")
  lazy val orcVersion: String = versionProperties.getProperty("orc.version")+" rev. "+svnRevision+(if (svnRevision.forall(_.isDigit)) "" else " (dev. build "+versionProperties.getProperty("orc.build.date")+" "+versionProperties.getProperty("orc.build.user")+")")
  lazy val orcURL: String = versionProperties.getProperty("orc.url")
  lazy val orcCopyright: String = "(c) "+copyrightYear+" "+versionProperties.getProperty("orc.vendor")
  lazy val copyrightYear: String = versionProperties.getProperty("orc.copyright-year")

  def setupLogging(options: OrcOptions) {
    val orcLogger = java.util.logging.Logger.getLogger("orc")
    val logLevel = java.util.logging.Level.parse(options.logLevel)
    orcLogger.setLevel(logLevel)
    val logHandler = new java.util.logging.ConsoleHandler() //FIXME:Allow other handlers, or none...
    logHandler.setLevel(logLevel)
    orcLogger.addHandler(logHandler)
    //TODO: orcLogger.config(options.printAllTheOptions...)
  }

  def printException(e: Throwable, err: PrintStream, showJavaStackTrace: Boolean) {
    e match {
      case je: JavaException if (!showJavaStackTrace) => err.print(je.getMessageAndPositon() + "\n" + je.getOrcStacktraceAsString())
      case oe: OrcException => err.print(oe.getMessageAndDiagnostics())
      case _ => e.printStackTrace(err)
    }
  }
}


/**
 * An OrcOptions that parses command line arguments passed with <code>parseCmdLine(args)</code>.
 *
 * @author jthywiss
 */
trait CmdLineOptions extends OrcOptions with CmdLineParser {
  StringOprd(()=>filename, filename=_, position = 0, argName = "file", required = true, usage = "Path to script to execute.")

  StringOpt(()=>logLevel, logLevel=_, ' ', "loglevel", usage = "Set the level of logging. Default is INFO. Allowed values: OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL")

  UnitOpt(()=>(!usePrelude), ()=>usePrelude=false, ' ', "noprelude", usage = "Do not implicitly include standard library (prelude), which is included by default.")

  StringListOpt(()=>includePath, includePath=_, 'I', "include-path", usage = "Set the include path for Orc includes (same syntax as CLASSPATH). Default is \".\", the current directory. Prelude files are always available for include regardless of this setting.")  

  StringListOpt(()=>additionalIncludes, additionalIncludes=_, ' ', "additional-includes", usage = "Include these files as if the program had include statements for them (same syntax as CLASSPATH). Default is none.")  

  StringListOpt(()=>classPath, classPath=_, ' ', "cp", usage = "Set the class path for Orc sites (same syntax as CLASSPATH). This is only used for classes not found in the Java VM classpath.")

  UnitOpt(()=>typecheck, ()=>typecheck=true, ' ', "typecheck", usage = "Enable typechecking, which is disabled by default.")

  UnitOpt(()=>disableRecursionCheck, ()=>disableRecursionCheck=true, ' ', "no-recursion-warn", usage = "Disable unguarded recursion check.")

  UnitOpt(()=>echoOil, ()=>echoOil=true, ' ', "echo-oil", usage = "Write the compiled program in OIL format to stdout.")

  FileOpt(()=>oilOutputFile.getOrElse(null), f => oilOutputFile=Some(f), 'o', "output-oil", usage = "Write the compiled program in OIL format to the given filename.")

  UnitOpt(()=>runOil, ()=>runOil=true, ' ', "run-oil", usage = "Attempt to parse the given program as an OIL file and run it. This performs no compilation steps.")
  
  UnitOpt(()=>compileOnly, ()=>compileOnly=true, 'c', "compile-only", usage = "Compile this program, but do not run it.")

  UnitOpt(()=>showJavaStackTrace, ()=>showJavaStackTrace=true, ' ', "java-stack-trace", usage = "Show Java stack traces on thrown Java exceptions.")

  UnitOpt(()=>disableTailCallOpt, ()=>disableTailCallOpt=true, ' ', "no-tco", usage = "Disable tail call optimization, for easier-to-debug stack traces.")

  IntOpt(()=>stackSize, stackSize=_, ' ', "stack-size", usage = "Terminate the program if this stack depth is exceeded. Default=infinity.")

  IntOpt(()=>maxTokens, maxTokens=_, ' ', "max-tokens", usage = "Terminate the program if more than this many tokens to be created. Default=infinity.")

  IntOpt(()=>maxSiteThreads, maxSiteThreads=_, ' ', "max-site-threads", usage = "Terminate the program if more than this many site calls are outstanding simultaneously. Default=infinity.")
}

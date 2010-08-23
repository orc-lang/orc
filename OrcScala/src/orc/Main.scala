//
// Main.scala -- Scala object Main
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 20, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import javax.script.{ScriptException, Compilable, ScriptEngine, ScriptEngineManager}
import javax.script.ScriptContext.ENGINE_SCOPE
import java.io.{PrintStream, FileNotFoundException, InputStreamReader, FileInputStream}
import scala.collection.JavaConversions._
import orc.error.OrcException
import orc.script.{OrcBindings, OrcScriptEngine}
import orc.values.Format
import orc.util.CmdLineParser
import orc.util.CmdLineUsageException
import orc.util.PrintVersionAndMessageException 

/**
 * A command-line tool invocation of the Orc compiler and runtime engine
 *
 * @author jthywiss
 */
object Main {

  def main(args: Array[String]) {
    class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions
    val engine = (new ScriptEngineManager).getEngineByName("orc").asInstanceOf[ScriptEngine with Compilable]
    if (engine == null) throw new ClassNotFoundException("Unable to load Orc ScriptEngine")
    try {
      val options = new OrcCmdLineOptions()
      options.parseCmdLine(args)
      engine.setBindings(options, ENGINE_SCOPE)
      val reader = new InputStreamReader(new FileInputStream(options.filename), "UTF-8")
      val compiledOrc = engine.compile(reader).asInstanceOf[OrcScriptEngine#OrcCompiledScript]
      val printPubs = new OrcEventAction() {
        override def published(value: AnyRef) { println(Format.formatValue(value)) }
        override def printed(s: String) { print(s) }
        override def caught(e: Throwable) { printException(e, Console.err) }
      }
      compiledOrc.run(printPubs)
    } catch {
      case e: CmdLineUsageException => Console.err.println("Orc: " + e.getMessage)
      case e: PrintVersionAndMessageException => println(orcImplName+" "+orcVersion+"\n"+orcURL+"\n"+orcCopyright+"\n\n"+e.getMessage)
      case e: FileNotFoundException => Console.err.println("Orc: File not found: " + e.getMessage)
      case e: ScriptException if (e.getCause == null) => Console.err.println(e.getMessage)
      case e: ScriptException => printException(e.getCause, Console.err)
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
  
  def printException(e: Throwable, err: PrintStream) {
    e match {
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

  IntOpt(()=>debugLevel, debugLevel=_, 'd', "debug", usage = "Enable debugging output, which is disabled by default. Higher numbers increase level of detail.")

  UnitOpt(()=>(!usePrelude), ()=>usePrelude=false, ' ', "noprelude", usage = "Do not implicitly include standard library (prelude), which is included by default.")

  StringListOpt(()=>includePath, includePath=_, 'I', "include-path", usage = "Set the include path for Orc includes (same syntax as CLASSPATH). Default is \".\", the current directory. Prelude files are always available for include regardless of this setting.")  

  UnitOpt(()=>exceptionsOn, ()=>exceptionsOn=true, ' ', "exceptions", usage = "Enable exceptions (experimental), which is disabled by default.")

  UnitOpt(()=>typecheck, ()=>typecheck=true, ' ', "typecheck", usage = "Enable typechecking, which is disabled by default.")

  IntOpt(()=>maxPublications, maxPublications=_, ' ', "pub", usage = "Terminate the program after this many values are published. Default=infinity.")

  StringListOpt(()=>classPath, classPath=_, ' ', "cp", usage = "Set the class path for Orc sites (same syntax as CLASSPATH). This is only used for classes not found in the Java VM classpath.")

  //UnitOpt({=true}, 'c', "noexecute", usage = "Compile this program, but do not run it.")
  //FileOpt(, 'o', "oilOut", usage = "Write the compiled OIL to the given filename.")
  //IntOpt(, ' ', "numSiteThreads", usage = "Use up to this many threads for blocking site calls. Default=2.")
}


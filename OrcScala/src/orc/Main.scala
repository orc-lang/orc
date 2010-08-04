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

import java.io.FileReader
import java.io.FileNotFoundException
import java.io.PrintStream
import orc.error.OrcException
import orc.script.OrcScriptEngine
import orc.script.OrcBindings
import orc.compile.parse.OrcReader
import orc.values.Format
import javax.script.ScriptEngineManager
import javax.script.ScriptEngine
import javax.script.Compilable
import javax.script.ScriptContext.ENGINE_SCOPE
import javax.script.ScriptException


/**
 * A command-line tool invocation of the Orc compiler and runtime engine
 *
 * @author jthywiss
 */
object Main {

  def main(args: Array[String]) {
    val engine = (new ScriptEngineManager).getEngineByName("orc").asInstanceOf[ScriptEngine with Compilable]
    if (engine == null) throw new ClassNotFoundException("Unable to load Orc ScriptEngine")
    try {
      val options = new OrcBindings() with CmdLineOptions
      options.parseCmdLine(args)
      engine.setBindings(options, ENGINE_SCOPE)
      val reader = new FileReader(options.filename)
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
    p.load(getClass().getResourceAsStream("version.properties"))
    p
  }
  lazy val orcImplName: String = versionProperties.getProperty("orc.title")
  lazy val svnRevision: String = versionProperties.getProperty("orc.svn-revision")
  lazy val orcVersion: String = versionProperties.getProperty("orc.version")+" rev. "+svnRevision+(if (svnRevision.forall(_.isDigit)) "" else " (dev. build "+versionProperties.getProperty("orc.build.date")+" "+versionProperties.getProperty("orc.build.user")+")")
  lazy val orcURL: String = versionProperties.getProperty("orc.url")
  lazy val orcCopyright: String = "(c) "+coyrightYear+" "+versionProperties.getProperty("orc.vendor")
  lazy val coyrightYear: String = versionProperties.getProperty("orc.copyright-year") 
  
  def printException(e: Throwable, err: PrintStream) {
    e match {
      case oe: OrcException => err.print(oe.getMessageAndDiagnostics())
      case _ => e.printStackTrace(err)
    }
  }
}

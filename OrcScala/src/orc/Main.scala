//
// Main.scala -- Scala object Main
// Project OrcScala
//
// Created by jthywiss on Jul 20, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import java.io.{ FileInputStream, FileNotFoundException, InputStreamReader, PrintStream }

import scala.collection.JavaConverters.{ asScalaBufferConverter, seqAsJavaListConverter }

import orc.error.OrcException
import orc.error.compiletime.CompilationException
import orc.error.runtime.JavaException
import orc.run.OrcDesktopEventAction
import orc.script.{ OrcBindings, OrcScriptEngine }
import orc.util.{ CmdLineParser, CmdLineUsageException, ExitStatus, MainExit, PrintVersionAndMessageException, UnrecognizedCmdLineOptArgException }
import orc.values.Format

import javax.script.{ ScriptEngine, ScriptEngineManager, ScriptException }
import javax.script.Compilable
import javax.script.ScriptContext.ENGINE_SCOPE

/** A command-line tool invocation of the Orc compiler and runtime engine
  *
  * @author jthywiss
  */
object Main extends MainExit {
  class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions

  def main(args: Array[String]): Unit = {
    haltOnUncaughtException()
    try {
      Logger.config(orcImplName + " " + orcVersion)
      val options = new OrcCmdLineOptions()
      options.parseCmdLine(args)
      Logger.config("Orc options & operands: " + options.composeCmdLine().mkString(" "))

      val engine = (new ScriptEngineManager).getEngineByName("orc").asInstanceOf[ScriptEngine with Compilable]
      if (engine == null) throw new ClassNotFoundException("Unable to load Orc ScriptEngine")
      engine.setBindings(options, ENGINE_SCOPE)

      val stream = new FileInputStream(options.filename)

      val compiledOrc =
        if (options.runOil) {
          engine.asInstanceOf[OrcScriptEngine[AnyVal]].loadDirectly(stream)
        } else {
          val reader = new InputStreamReader(stream, "UTF-8")
          engine.compile(reader).asInstanceOf[OrcScriptEngine[AnyVal]#OrcCompiledScript]
        }

      if (options.compileOnly) {
        if (options.runOil) {
          Console.err.println("Warning: run-oil ignored since compile-only was also set.")
        }
        return
      }

      val printPubs = new OrcDesktopEventAction() {
        override def published(value: AnyRef) {
          println(Format.formatValue(value))
          Console.out.flush()
        }
        override def caught(e: Throwable) {
          Console.out.flush()
          printException(e, Console.err, options.showJavaStackTrace)
          Console.err.flush()
        }
      }
      compiledOrc.run(printPubs)
    } catch {
      /* Exceptions caught here don't get logged or stack traces printed */
      case e: PrintVersionAndMessageException => println(orcImplName + " " + orcVersion + "\n" + orcURL + "\n" + orcCopyright + "\n\n" + e.getMessage)
      case e: FileNotFoundException => failureExit("File not found: " + e.getMessage, ExitStatus.NoInput)
      case e: ScriptException if (e.getCause == null) => { Console.err.println(e.getMessage); System.exit(ExitStatusRunFail) }
      case e: ScriptException if (e.getCause.isInstanceOf[CompilationException]) => System.exit(ExitStatusCompileFail) // Ignore compilation errors. They will already have been printed.
      case e: ScriptException => printException(e.getCause, Console.err, false); System.exit(ExitStatusRunFail)
      case e: CmdLineUsageException => failureExit(e.getMessage, ExitStatus.Usage)
      case e: java.net.UnknownHostException => failureExit(e.toString, ExitStatus.NoHost)
      case e: java.net.ConnectException => failureExit(e.toString, ExitStatus.Unavailable)
      case e: java.io.IOException => failureExit(e.toString, ExitStatus.IoErr)
    }
  }

  private val ourUEH: PartialFunction[Throwable, Unit] = {
    /* If a ScriptException is thrown on another thread, print it to stderr and set the exit status code. */
    case e: ScriptException if (e.getCause == null) => { Console.err.println(e.getMessage); System.exit(ExitStatusRunFail) }
    case e: ScriptException if (e.getCause.isInstanceOf[CompilationException]) => { System.exit(ExitStatusCompileFail) } // Ignore compilation errors. They will already have been printed.
    case e: ScriptException => { printException(e.getCause, Console.err, false); System.exit(ExitStatusRunFail) }
  }
  val mainUncaughtExceptionHandler = ourUEH orElse basicUncaughtExceptionHandler

  /* Exit status codes */
  val ExitStatusRunFail = 1
  val ExitStatusCompileFail = 2

  val versionProperties = {
    val p = new java.util.Properties()
    val vp = getClass().getResourceAsStream("version.properties")
    if (vp == null) throw new java.util.MissingResourceException("Unable to load version.properties resource", "/orc/version.properties", "")
    p.load(vp)
    p
  }
  lazy val orcImplName: String = versionProperties.getProperty("orc.title")
  lazy val scmRevision: String = versionProperties.getProperty("orc.scm-revision")
  lazy val orcVersion: String = versionProperties.getProperty("orc.version") + " rev. " + scmRevision + " (built " + versionProperties.getProperty("orc.build.date") + " " + versionProperties.getProperty("orc.build.user") + ")"
  lazy val orcURL: String = versionProperties.getProperty("orc.url")
  lazy val orcCopyright: String = "(c) " + copyrightYear + " " + versionProperties.getProperty("orc.vendor")
  lazy val copyrightYear: String = versionProperties.getProperty("orc.copyright-year")

  def printException(e: Throwable, err: PrintStream, showJavaStackTrace: Boolean) {
    e match {
      case je: JavaException if (!showJavaStackTrace) => err.print(je.getMessageAndPositon() + "\n" + je.getOrcStacktraceAsString())
      case oe: OrcException => err.print(oe.getMessageAndDiagnostics())
      case _ => e.printStackTrace(err)
    }
  }
}

/** An OrcOptions that parses command line arguments passed with <code>parseCmdLine(args)</code>.
  *
  * @author jthywiss
  */
trait CmdLineOptions extends OrcOptions with CmdLineParser {
  StringOprd(() => filename, filename = _, position = 0, argName = "file", required = true, usage = "Path to script to execute.")

  UnitOpt(() => (!usePrelude), () => usePrelude = false, ' ', "noprelude", usage = "Do not implicitly include standard library (prelude), which is included by default.")

  StringListOpt(() => includePath.asScala, ip => includePath = ip.asJava, 'I', "include-path", usage = "Set the include path for Orc includes (same syntax as CLASSPATH). Default is \".\", the current directory. Prelude files are always available for include regardless of this setting.")

  StringListOpt(() => additionalIncludes.asScala, ais => additionalIncludes = ais.asJava, ' ', "additional-includes", usage = "Include these files as if the program had include statements for them (same syntax as CLASSPATH). Default is none.")

  StringListOpt(() => classPath.asScala, cp => classPath = cp.asJava, ' ', "cp", usage = "Set the class path for Orc sites (same syntax as CLASSPATH). This is only used for classes not found in the Java VM classpath.")

  UnitOpt(() => typecheck, () => typecheck = true, ' ', "typecheck", usage = "Enable typechecking, which is disabled by default.")

  UnitOpt(() => disableRecursionCheck, () => disableRecursionCheck = true, ' ', "no-recursion-warn", usage = "Disable unguarded recursion check.")

  UnitOpt(() => echoOil, () => echoOil = true, ' ', "echo-oil", usage = "Write the compiled program in OIL format to stdout.")

  IntOpt(() => echoIR, echoIR = _, ' ', "echo-ir", usage = "Write selected program intermediate representations to the stdout. The argument is a bitmask. So, 0 means echo nothing, or -1 means echo all.")

  FileOpt(() => oilOutputFile.getOrElse(null), f => oilOutputFile = Some(f), 'o', "output-oil", usage = "Write the compiled program in OIL format to the given filename.")

  UnitOpt(() => runOil, () => runOil = true, ' ', "run-oil", usage = "Attempt to parse the given program as an OIL file and run it. This performs no compilation steps.")

  UnitOpt(() => compileOnly, () => compileOnly = true, 'c', "compile-only", usage = "Compile this program, but do not run it.")

  UnitOpt(() => showJavaStackTrace, () => showJavaStackTrace = true, ' ', "java-stack-trace", usage = "Show Java stack traces on thrown Java exceptions.")

  UnitOpt(() => disableTailCallOpt, () => disableTailCallOpt = true, ' ', "no-tco", usage = "Disable tail call optimization, for easier-to-debug stack traces.")

  IntOpt(() => stackSize, stackSize = _, ' ', "stack-size", usage = "Terminate the program if this stack depth is exceeded. Default=infinity.")

  IntOpt(() => maxTokens, maxTokens = _, ' ', "max-tokens", usage = "Terminate the program if more than this many tokens to be created. Default=infinity.")

  IntOpt(() => maxSiteThreads, maxSiteThreads = _, ' ', "max-site-threads", usage = "Limit the number of simultaneously outstanding site calls to this number. Default=infinity.")

  StringOpt(() => backend.toString, arg =>
    backend = BackendType.fromStringOption(arg) match {
      case Some(b) => b
      case None => throw new UnrecognizedCmdLineOptArgException(s"""Backend "${arg}" does not exist or is not supported.""", "backend", arg, this)
    }, ' ', "backend", usage = "Set the backend to use for compilation and execution. Allowed values: " + BackendType.backendTypes.map(_.id).mkString(", ") + ". Default is \"token\".")

  StringListOpt(() => optimizationOptions.asScala, oo => optimizationOptions = oo.asJava, ' ', "opt-opt", separator = ",",
    usage = "Provide option for use by the optimizers separated by commas. Options in the form '[optimizer-name]' and '-[optimizer-name]=off' enable and disable optimizers. Other options are arbitrary key-value pairs used by the optimizer (the value defaults to 'true').")

  IntOpt(() => optimizationLevel, optimizationLevel = _, 'O', "optimize", usage = "Set a general optimization level. This selects a set of default optimization options. --opt-opt may be used to override these default.")

  SocketOpt(() => listenSocketAddress, listenSocketAddress = _, ' ', "listen", usage = "Local socket address (host:port) to listen on. Default is to listen on a random free dynamic port on all local interfaces.")

  IntOpt(() => followerCount, followerCount = _, ' ', "follower-count", usage = "Wait for this number of followers to join cluster before running program. Default is 0.")

  FileOpt(() => listenSockAddrFile.getOrElse(null), f => listenSockAddrFile = Some(f), ' ', "listen-sockaddr-file", usage = "Write the actual bound listen socket address to this file. Useful when listening on a random port.")
}

//
// OrcForTesting.scala -- Scala object OrcForTesting
// Project OrcTests
//
// $Id$
//
// Created by jthywiss on Jul 22, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.test

import java.io.{ FileInputStream, FileNotFoundException, InputStreamReader }
import java.util.concurrent.TimeoutException

import orc.{ OrcEvent, OrcEventAction }
import orc.error.OrcException
import orc.lib.str.PrintEvent
import orc.script.{ OrcBindings, OrcScriptEngine }
import orc.util.SynchronousThreadExec
import orc.values.Format

import javax.script.{ ScriptEngine, ScriptEngineManager, ScriptException }
import javax.script.Compilable
import javax.script.ScriptContext.ENGINE_SCOPE

/** A test harness for the standard Orc compiler and runtime
  * engine, as exposed through the JSR-223 interface.
  *
  * @author jthywiss
  */
object OrcForTesting {
  @throws(classOf[ClassNotFoundException])
  @throws(classOf[FileNotFoundException])
  @throws(classOf[OrcException])
  def compile(filename: String, options: OrcBindings): OrcScriptEngine[AnyRef]#OrcCompiledScript = {
    val engine = (new ScriptEngineManager).getEngineByName("orc").asInstanceOf[ScriptEngine with Compilable]
    if (engine == null) throw new ClassNotFoundException("Unable to load Orc ScriptEngine")
    var reader : InputStreamReader = null
    try {
      options.filename = filename
      engine.setBindings(options, ENGINE_SCOPE)
      reader = new InputStreamReader(new FileInputStream(options.filename), "UTF-8")
      engine.compile(reader).asInstanceOf[OrcScriptEngine[AnyRef]#OrcCompiledScript]
    } catch {
      case e: ScriptException if (e.getCause != null) => throw e.getCause // un-wrap and propagate
    } finally {
      if(reader != null)
        reader.close()
      Console.out.flush()
      Console.err.flush()
    }
  }
  
  def importScript(filename: String, options: OrcBindings, script: OrcScriptEngine[AnyRef]#OrcCompiledScript): OrcScriptEngine[AnyRef]#OrcCompiledScript = {
    val engine = (new ScriptEngineManager).getEngineByName("orc").asInstanceOf[OrcScriptEngine[AnyRef]]
    if (engine == null) throw new ClassNotFoundException("Unable to load Orc ScriptEngine")
    try {
      options.filename = filename
      engine.setBindings(options, ENGINE_SCOPE)
      engine.importLoaded(script)
    } catch {
      case e: ScriptException if (e.getCause != null) => throw e.getCause // un-wrap and propagate
    }
  }

  @throws(classOf[OrcException])
  @throws(classOf[TimeoutException])
  def run(compiledScript: OrcScriptEngine[AnyRef]#OrcCompiledScript, timeout: Long): String = {
    try {
      val output = new StringBuffer()
      var lastError: Throwable = null
      val eventActions = new OrcEventAction() {
        override def published(value: AnyRef) {
          output.append(Format.formatValue(value) + "\n")
          println(Format.formatValue(value))
          Console.out.flush()
        }
        override def caught(e: Throwable) {
          val name = Option(e.getClass.getCanonicalName).getOrElse(e.getClass.getName)
          val msg = "Error: " + name + ": " + e.getMessage()
          println(msg)
          Console.out.flush()
          output.append(msg + "\n")
          if (e.isInstanceOf[Error]) lastError = e
        }
        override def other(event: OrcEvent) {
          event match {
            case PrintEvent(text) => {
              print(text)
              Console.out.flush()
              output.append(text)
            }
            case e => super.other(e)
          }
        }

      }

      // run the engine with a fixed timeout
      SynchronousThreadExec("Orc Test Main Thread", timeout * 1000L, {
        compiledScript.run(eventActions)
        /* SynchronousThreadExec will interrupt thread after timeout.
         * SupportForSynchronousExecution.runSynchronous will stop the
         * engine on an interrupt of the main thread. */
      })

      if (lastError != null) throw lastError
      output.toString
    } catch {
      case e: ScriptException => throw e.getCause // un-wrap and propagate
    } finally {
      Console.out.flush();
      Console.err.flush();
    }
  }

  @throws(classOf[ClassNotFoundException])
  @throws(classOf[FileNotFoundException])
  @throws(classOf[OrcException])
  @throws(classOf[TimeoutException])
  def compileAndRun(filename: String, timeout: Long, bindings: OrcBindings = new OrcBindings()): String = {
    run(compile(filename, bindings), timeout)
  }
}

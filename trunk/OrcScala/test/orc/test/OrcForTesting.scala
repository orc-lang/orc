//
// OrcForTesting.scala -- Scala class/trait/object OrcForTesting
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 22, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.test

import java.io.FileReader
import java.io.FileNotFoundException
import java.util.concurrent.FutureTask
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutionException
import javax.script.ScriptEngineManager
import javax.script.ScriptEngine
import javax.script.Compilable
import javax.script.ScriptContext.ENGINE_SCOPE
import javax.script.ScriptException
import orc.script.OrcBindings
import orc.script.OrcScriptEngine
import orc.OrcEventAction
import orc.values.Format
import orc.error.OrcException


/**
 * A test harness for the standard Orc compiler and runtime
 * engine, as exposed through the JSR-223 interface.
 *
 * @author jthywiss
 */
object OrcForTesting {
  @throws(classOf[ClassNotFoundException])
  @throws(classOf[FileNotFoundException])
  @throws(classOf[OrcException])
  def compile(filename: String): OrcScriptEngine#OrcCompiledScript = {
    val engine = (new ScriptEngineManager).getEngineByName("orc").asInstanceOf[ScriptEngine with Compilable]
    if (engine == null) throw new ClassNotFoundException("Unable to load Orc ScriptEngine")
    try {
      val options = new OrcBindings()
      options.filename = filename
      engine.setBindings(options, ENGINE_SCOPE)
      val reader = new FileReader(options.filename)
      engine.compile(reader).asInstanceOf[OrcScriptEngine#OrcCompiledScript]
    } catch {
      case e: ScriptException => throw e.getCause // un-wrap and propagate
    }
  }

  @throws(classOf[OrcException])
  @throws(classOf[TimeoutException])
  def run(compiledScript: OrcScriptEngine#OrcCompiledScript, timeout: Long): String = {
    try {
      val output = new StringBuilder()
      val eventActions = new OrcEventAction() {
        override def published(value: AnyRef) {
          output.append(Format.formatValue(value)+"\n")
          println(Format.formatValue(value))
        }
        override def printed(s: String) { print(s); output.append(s) }
        override def caught(e: Throwable) { output.append("Error: "+e.getClass().getName()+": "+e.getMessage()+"\n") }
      }

      // run the engine with a fixed timeout
      val future = new FutureTask[Unit](new Callable[Unit]() {
        def call() { compiledScript.run(eventActions) }
      });
      new Thread(future, "Orc engine").start();
      try {
        try {
          future.get(timeout, SECONDS);
        } catch {
          case e: TimeoutException => {
            future.cancel(true)
            throw e
          }
        }
      } catch {
        case e: ExecutionException => throw e.getCause()
      }

      output.toString
    } catch {
      case e: ScriptException => throw e.getCause // un-wrap and propagate
    }
  }
  
  @throws(classOf[ClassNotFoundException])
  @throws(classOf[FileNotFoundException])
  @throws(classOf[OrcException])
  @throws(classOf[TimeoutException])
  def compileAndRun(filename: String, timeout: Long): String = {
      run(compile(filename), timeout)
  }
}

//
// PorcBackend.scala -- Scala class PorcBackend
// Project OrcScala
//
// Created by amp on Aug 28, 2013.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import java.io.{ ObjectInputStream, ObjectOutputStream }

import orc.ast.porc.MethodCPS
import orc.compile.orctimizer.PorcOrcCompiler
import orc.compile.parse.OrcInputContext
import orc.error.compiletime.CompileLogger
import orc.error.loadtime.{ DeserializationTypeException, LoadingException }
import orc.progress.ProgressMonitor

/** A backend implementation using the Orctimizer and Porc compilers.
  *
  * This is designed to be extended with a runtime which takes Porc as input.
  *
  * @author amp
  */
abstract class PorcBackend extends Backend[MethodCPS] {
  lazy val compiler: Compiler[MethodCPS] = new PorcOrcCompiler() with Compiler[MethodCPS] {
    def compile(source: OrcInputContext, options: OrcCompilationOptions,
      compileLogger: CompileLogger, progress: ProgressMonitor): MethodCPS = {
      this(source, modifyCompilationOptions(options), compileLogger, progress)
    }
  }
  
  protected def modifyCompilationOptions(options: OrcCompilationOptions): OrcCompilationOptions = options

  // NOTE: If needed we could implement an XML serializer for Porc. We could also make things even simpler by just using java serialization here.
  val serializer: Option[CodeSerializer[MethodCPS]] = Some(new CodeSerializer[MethodCPS] {   
    /** Generate a serialized form from <code>code</code>.
      */
    def serialize(code: MethodCPS, out: java.io.OutputStream): Unit = {
      val oout = new ObjectOutputStream(out)
      oout.writeObject(code)
    }
  
    /** Take a serialized form and rebuild the original compiled code object.
      */
    @throws(classOf[LoadingException])
    def deserialize(in: java.io.InputStream): MethodCPS = {
      val oin = new ObjectInputStream(in)
      val o = oin.readObject()
      o match {
        case m: MethodCPS =>
          m
        case _ =>
          throw new DeserializationTypeException("Loaded data was of the incorrect format.")
      }
    }
  })


  def createRuntime(options: OrcExecutionOptions): Runtime[MethodCPS]
}

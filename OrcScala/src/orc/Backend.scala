//
// Backend.scala -- Abstract trait representing a compiler and runtime
// Project OrcScala
//
// Created by Arthur Peters on Aug 28, 2013.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import orc.compile.parse.OrcInputContext
import orc.error.compiletime.CompileLogger
import orc.progress.ProgressMonitor
import orc.error.loadtime.LoadingException
import java.io.IOException
import java.io.Writer
import java.io.File
import java.io.BufferedReader
import orc.error.compiletime.PrintWriterCompileLogger
import java.io.PrintWriter
import orc.error.runtime.ExecutionException
import orc.progress.NullProgressMonitor
import orc.ast.oil.nameless.Expression
import orc.error.compiletime.ExceptionCollectingCompileLogger
import orc.error.compiletime.CompilationException

/** An enumeration over the supported backends.
  */
sealed trait BackendType {
  def newBackend(): Backend[_]
}

object BackendType {
  private val stringToBackendType = Map[String, BackendType](
    TokenInterpreterBackend.toString -> TokenInterpreterBackend,
    DistributedBackendType.toString -> DistributedBackendType,
    PorcCompilerBackend.toString -> PorcCompilerBackend)

  def knownBackendNames = stringToBackendType.keys

  def fromString(s: String) = {
    fromStringOption(s).getOrElse(TokenInterpreterBackend)
  }
  def fromStringOption(s: String) = {
    stringToBackendType.get(s.toLowerCase)
  }
}

/** The standard token interpreter's BackendType. */
case object TokenInterpreterBackend extends BackendType {
  override val toString = "token"
  override def newBackend(): Backend[Expression] = new StandardBackend()
}

/** The Distributed Orc BackendType. */
case object DistributedBackendType extends BackendType {
  override val toString = "distrib"
  override def newBackend(): Backend[Expression] = new DistributedBackend()
}

/** The target based on the Orctimizer and Porc.
  */
case object PorcCompilerBackend extends BackendType {
  override val toString = "porc"
  override def newBackend(): Backend[PorcBackend.CompiledOrcProgram] = new PorcBackend()
}

/** This represents an abstract Orc compiler. It generates an opaque code object that can be
  * executed on a matching Runtime.
  *
  * @see Backend
  * @author amp
  */
trait Compiler[+CompiledCode] {
  /** Compile <code>source</code> using the given options. The resulting CompiledCode object can
    * be executed on a runtime acquired from the same Backend.
    */
  @throws(classOf[IOException])
  def compile(source: OrcInputContext, options: OrcCompilationOptions,
    compileLogger: CompileLogger, progress: ProgressMonitor): CompiledCode

  private class OrcReaderInputContext(val javaReader: java.io.Reader, override val descr: String) extends OrcInputContext {
    val file = new File(descr)
    override val reader = orc.compile.parse.OrcReader(this, new BufferedReader(javaReader))
    override def toURI = file.toURI
    override def toURL = toURI.toURL
  }

  class ManyCompilationExceptions(exceptions: Seq[Throwable]) extends CompilationException("Multiple compiler errors") {
    exceptions.foreach(addSuppressed)
  }

  /** Compile the code in the reader using the given options and produce error messages on the
    * err writer. This is a simple wrapper around the other compile function.
    */
  @throws(classOf[IOException]) @throws(classOf[CompilationException])
  def compile(source: java.io.Reader, options: OrcCompilationOptions, err: Writer): CompiledCode = {
    val logger = new ExceptionCollectingCompileLogger(new PrintWriter(err, true))
    val res = compile(new OrcReaderInputContext(source, options.filename), options,
      logger, NullProgressMonitor)
    logger.exceptions match {
      case Seq() =>
        res
      case Seq(e: CompilationException) =>
        throw e
      case es =>
        throw new ManyCompilationExceptions(es)
    }
  }
}

/** This represents an abstract Orc runtime. It executes compiled code returned by the matching
  * compiler.
  *
  * @see Backend
  * @author amp
  */
trait Runtime[-CompiledCode] {
  /** Execute <code>code</code> asynchronously. This call will return immediately.
    * <code>eventHandler</code> will be called from many threads.
    */
  @throws(classOf[ExecutionException])
  def run(code: CompiledCode, eventHandler: OrcEvent => Unit): Unit

  /** Execute <code>code</code> synchronously not returning until execution is complete.
    * <code>eventHandler</code> will be called from many threads, but will not be called after
    * this call returns.
    */
  @throws(classOf[ExecutionException])
  @throws(classOf[InterruptedException])
  def runSynchronous(code: CompiledCode, eventHandler: OrcEvent => Unit): Unit

  /** Stop the runtime and any currently running executions. Once this is called all
    * other calls to this runtime will fail.
    */
  def stop(): Unit
}

/** A class that marshals compiled code into and out of XML. The code can be unmarshaled
  * and used on other computers or at different times as long as the same backend is used.
  *
  * @see Backend
  * @author amp
  */
trait CodeSerializer[CompiledCode] {
  /** Generate a serialized form from <code>code</code>.
    */
  def serialize(code: CompiledCode, out: java.io.OutputStream): Unit

  /** Take a serialized form and rebuild the original compiled code object.
    */
  @throws(classOf[LoadingException])
  def deserialize(in: java.io.InputStream): CompiledCode
}

/** A backend is a combination of a compiler and a matching runtime.
  *
  * @author amp
  */
trait Backend[CompiledCode] {
  /** Get a compiler instance for this backend. It is thread-safe, and may or may not be
    * shared by other calls to this function.
    */
  def compiler: Compiler[CompiledCode]

  /** Get the XML serializer for code of this backend if there is one. Backends are not
    * required to provide XML support.
    */
  def serializer: Option[CodeSerializer[CompiledCode]]

  /** Create a new runtime instance using the given options. The runtime can be used to
    * execute more than one program, but its options cannot be changed.
    */
  def createRuntime(options: OrcExecutionOptions): Runtime[CompiledCode]
}

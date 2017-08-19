//
// Backend.scala -- Abstract trait representing a compiler and runtime
// Project OrcScala
//
// Created by Arthur Peters on Aug 28, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import java.io.{ BufferedReader, File, IOException, PrintWriter, Writer }
import java.util.ServiceLoader

import orc.ast.oil.nameless.Expression
import orc.compile.parse.OrcInputContext
import orc.error.compiletime.{ CompilationException, CompileLogger, ExceptionCollectingCompileLogger, ManyCompilationExceptions, PrintWriterCompileLogger }
import orc.error.loadtime.LoadingException
import orc.error.runtime.ExecutionException
import orc.progress.{ NullProgressMonitor, ProgressMonitor }

/** An enumeration over the supported backends.
  */
abstract class BackendType {
  type CompiledCode
  
  def name: String
  final def id: String = name.toLowerCase()
  def newBackend(): Backend[CompiledCode]
  override def toString = name
}

object BackendType {
  private val serviceLoader: ServiceLoader[BackendType] = ServiceLoader.load(classOf[BackendType])
  
  val backendTypes: Iterable[BackendType] = {
    import collection.JavaConverters._
    serviceLoader.iterator().asScala.toSeq.reverse
  }

  private val stringToBackendType: Map[String, BackendType] = {
    backendTypes.foldLeft(Map[String, BackendType]()) { (map, typ) =>
      if(map contains typ.id) {
        Logger.warning(s"Ignoring backend ${typ.getClass.getCanonicalName} (defined in ${typ.getClass.getClassLoader}), because the backend id ${typ.id} is already used by ${map(typ.id).getClass.getCanonicalName} (defined in ${map(typ.id).getClass.getClassLoader}).")
        map
      } else {
        map + (typ.id -> typ)
      }
    }
  }

  def fromString(s: String): BackendType = {
    fromStringOption(s).getOrElse {
      throw new IllegalArgumentException(s"""Backend "${s}" does not exist or is not supported.""")
    }
  }
  def fromStringOption(s: String) = {
    stringToBackendType.get(s.toLowerCase)
  }
}

/** The standard token interpreter's BackendType. */
case class TokenInterpreterBackend() extends BackendType {
  type CompiledCode = Expression
  
  val name = "token"
  def newBackend(): Backend[Expression] = new StandardBackend()
}

/** The Distributed Orc BackendType. */
case class DistributedBackendType() extends BackendType {
  type CompiledCode = Expression
  
  val name = "distrib"
  def newBackend(): Backend[Expression] = new DistributedBackend()
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
  // TODO: Because compile does not throw on failure it should return options or have the null return documented like WOW.

  private class OrcReaderInputContext(val javaReader: java.io.Reader, override val descr: String) extends OrcInputContext {
    val file = new File(descr)
    override val reader = orc.compile.parse.OrcReader(this, new BufferedReader(javaReader))
    override def toURI = file.toURI
    override def toURL = toURI.toURL
  }

  /** Compile the code in the reader using the given options and produce error messages on the
    * err writer and throwing an exception if any errors occurred.
    *
    * It is used by the testing framework to allow better errors.
    */
  @throws(classOf[IOException]) @throws(classOf[CompilationException])
  def compileExceptionOnError(source: java.io.Reader, options: OrcCompilationOptions, err: Writer): CompiledCode = {
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

  /** Compile the code in the reader using the given options and produce error messages on the
    * err writer and throwing an exception if any errors occurred.
    *
    * It is used by the testing framework to allow better errors.
    */
  @throws(classOf[IOException])
  def compileLogOnError(source: java.io.Reader, options: OrcCompilationOptions, err: Writer): CompiledCode = {
    val logger = new PrintWriterCompileLogger(new PrintWriter(err, true))
    compile(new OrcReaderInputContext(source, options.filename), options,
      logger, NullProgressMonitor)
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

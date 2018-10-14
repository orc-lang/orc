//
// PorcELanguageBase.scala -- Truffle language implementation PorcELanguageBase
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce

import java.io.{ BufferedReader, File, PrintStream, PrintWriter }

import orc.{ OrcOptions, PorcEBackend, PorcEPolyglotLauncher }
import orc.compile.parse.OrcInputContext
import orc.run.porce.runtime.{ Counter, PorcEClosure, PorcEObject, PorcERuntime, Terminator }
import orc.script.OrcBindings

import com.oracle.truffle.api.{ CallTarget, Scope, Truffle, TruffleLanguage }
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.interop.{ ForeignAccess, TruffleObject }
import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.source.Source

class PorcELanguageBase extends TruffleLanguage[PorcEContext] {
    this: PorcELanguage =>

  val options: OrcOptions = PorcEPolyglotLauncher.orcOptions.getOrElse(new OrcBindings())
  val backend = PorcEBackend(this)
  val compiler = backend.compiler
  val runtime: PorcERuntime with orc.Runtime[orc.ast.porc.MethodCPS] = backend.createRuntime(options).asInstanceOf[PorcERuntime with orc.Runtime[orc.ast.porc.MethodCPS]]

  def printException(e: Throwable, err: PrintStream, showJavaStackTrace: Boolean) {
    e match {
      case je: orc.error.runtime.JavaException if (!showJavaStackTrace) => err.print(je.getMessageAndPositon() + "\n" + je.getOrcStacktraceAsString())
      case oe: orc.error.OrcException => err.print(oe.getMessageAndDiagnostics())
      case _ => e.printStackTrace(err)
    }
  }

  val eventHandler = new orc.run.OrcDesktopEventAction() {
    override def published(value: AnyRef) {
      println(orc.values.Format.formatValue(value))
      Console.out.flush()
    }
    override def caught(e: Throwable) {
      Console.out.flush()
      printException(e, Console.err, options.showJavaStackTrace)
      Console.err.flush()
    }
  }

  override def createContext(env: com.oracle.truffle.api.TruffleLanguage.Env): PorcEContext = {
    new PorcEContext(runtime)
  }

  override def initializeThread(ctx: PorcEContext, thread: Thread) = {
    assert(ctx.thread == null)
    // println(s"initializeThread: $thread $ctx")
    ctx.thread = thread
    ctx.depth = 0
  }
  override def disposeThread(ctx: PorcEContext, thread: Thread) = {
    assert(ctx.thread == thread)
    //println(s"disposeThread: $thread $ctx")
    ctx.thread = null
    ctx.depth = 0
  }

  private class SourceInputContext(val source: Source, override val descr: String) extends OrcInputContext {
    val file = new File(descr)
    override val reader = orc.compile.parse.OrcReader(this, new BufferedReader(source.getReader()))
    override def toURI = source.getURI
    override def toURL = toURI.toURL
  }

  @throws[Exception]
  override def parse(request: TruffleLanguage.ParsingRequest): CallTarget = {
    import orc.error.compiletime.PrintWriterCompileLogger
    import orc.progress.NullProgressMonitor

    val logger = new PrintWriterCompileLogger(new PrintWriter(System.err, true))
    val code = compiler.compile(new SourceInputContext(request.getSource,
      new File(request.getSource.getURI).getCanonicalPath()), options,
      logger, NullProgressMonitor)
    val launchNode = new RootNode(this) {
      def execute(frame: VirtualFrame): Object = {
        runtime.runSynchronous(code, eventHandler.asFunction)
        // Create an object which cannot do anything as the "Signal" return value.
        new TruffleObject {
          self =>
          def getForeignAccess(): ForeignAccess = {
            ForeignAccess.create(new ForeignAccess.Factory {
              def accessMessage(tree: com.oracle.truffle.api.interop.Message): CallTarget = null
              def canHandle(obj: TruffleObject): Boolean = obj == self
            })
          }
        }
      }
    }
    Truffle.getRuntime().createCallTarget(launchNode)
  }

  override def findTopScopes(context: PorcEContext): java.lang.Iterable[Scope] = {
    java.util.Collections.emptyList()
  }

  override def isObjectOfLanguage(obj: AnyRef): Boolean = {
    obj match {
      case _: PorcEObject | _: PorcEClosure | _: Counter | _: Terminator =>
        true
      case _ => false
    }
  }
}

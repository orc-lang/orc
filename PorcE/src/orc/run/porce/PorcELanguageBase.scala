package orc.run.porce

import java.io.BufferedReader
import java.io.File
import java.io.PrintWriter

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.CallTarget
import com.oracle.truffle.api.TruffleLanguage
import com.oracle.truffle.api.source.Source
import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.frame.VirtualFrame

import orc.PorcEBackend
import orc.compile.parse.OrcInputContext
import orc.run.porce.runtime.Counter
import orc.run.porce.runtime.PorcEClosure
import orc.run.porce.runtime.PorcEObject
import orc.run.porce.runtime.Terminator
import orc.script.OrcBindings
import java.io.PrintStream
import orc.PorcEPolyglotLauncher
import orc.OrcOptions
import orc.run.porce.runtime.PorcERuntime

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
    println(s"initializeThread: $thread $ctx")
    ctx.thread = thread
    ctx.depth = 0
  }
  override def disposeThread(ctx: PorcEContext, thread: Thread) = {
    assert(ctx.thread == thread)
    println(s"disposeThread: $thread $ctx")
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
        null
      }
    }
    Truffle.getRuntime().createCallTarget(launchNode)
  }

  override def findExportedSymbol(context: PorcEContext, globalName: String,
    onlyExplicit: Boolean): AnyRef = {
    return null;
  }

  override def getLanguageGlobal(context: PorcEContext): AnyRef = {
    return null;
  }

  override def isObjectOfLanguage(obj: AnyRef): Boolean = {
    obj match {
      case _: PorcEObject | _: PorcEClosure | _: Counter | _: Terminator =>
        true
      case _ => false
    }
  }
}

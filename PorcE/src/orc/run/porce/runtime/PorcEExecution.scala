package orc.run.porce.runtime

import orc.OrcEvent
import orc.ExecutionRoot
import orc.run.core.EventHandler
import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.frame.VirtualFrame
import orc.PublishedEvent
import orc.run.porce.PorcEUnit
import orc.run.porce.Logger
import com.oracle.truffle.api.CallTarget
import orc.Schedulable
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import java.util.logging.Level

class PorcEExecution(val runtime: PorcERuntime, protected var eventHandler: OrcEvent => Unit) extends ExecutionRoot with EventHandler {
  private var _isDone = false
  
  /** Block until this context halts.
    */
  def waitForHalt(): Unit = {
    synchronized {
      while (!_isDone) wait()
    }
  }
  
  def isDone = PorcEExecution.this.synchronized { _isDone }

  runtime.installHandlers(this)

	@TruffleBoundary
  def notifyOrcWithBoundary(e: OrcEvent) = {
    notifyOrc(e)
  }

  val p: PorcEClosure = {
    Utilities.PorcEClosure(new RootNode(null) {
      def execute(frame: VirtualFrame): Object = {
        // Skip the first argument since it is our captured value array.
        val v = frame.getArguments()(1)
        notifyOrcWithBoundary(PublishedEvent(v))
        // Token: from initial caller of p.
        c.haltToken()
        PorcEUnit.SINGLETON
      }      
    })
  }

  val c: Counter = new Counter {
    def onResurrect() = {
      throw new AssertionError("The top-level counter can never be resurrected.")
    }
    
    def onHalt(): Unit = {
      // Runs regardless of discorporation.
      Logger.fine("Top level context complete.")
      runtime.removeRoot(PorcEExecution.this)
      PorcEExecution.this.synchronized {
        PorcEExecution.this._isDone = true
        PorcEExecution.this.notifyAll()
      }
    }
  }

  val t = new Terminator

  def scheduleProgram(prog: PorcEClosure): Unit = {
    val nStarts = System.getProperty("porce.nStarts", "1").toInt
    // Token: From initial.
    for(_ <- 0 until nStarts) {
      c.newToken()
      runtime.schedule(CallClosureSchedulable.varArgs(prog, Array(null, p, c, t)))
    }
    c.haltToken()
    
    if(CounterConstants.tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      Thread.sleep(10000)
      Counter.report()
    }
  }
}
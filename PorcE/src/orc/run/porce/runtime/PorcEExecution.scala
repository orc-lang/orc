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

class PorcEExecution(val runtime: PorcERuntime, protected var eventHandler: OrcEvent => Unit) extends ExecutionRoot with EventHandler {
  private var isDone = false
  
  /** Block until this context halts.
    */
  def waitForHalt(): Unit = {
    synchronized {
      while (!isDone) wait()
    }
  }

  runtime.installHandlers(this)

	@TruffleBoundary(allowInlining = true)
  def notifyOrcWithBoundary(e: OrcEvent) = {
    notifyOrc(e)
  }

  val p: PorcEClosure = {
    Utilities.PorcEClosure(new RootNode(null) {
      def execute(frame: VirtualFrame): Object = {
        // Skip the first argument since it is our captured value array.
        val v = frame.getArguments()(1)
        notifyOrcWithBoundary(PublishedEvent(v))
        PorcEUnit.SINGLETON
      }      
    })
  }

  val c: Counter = new Counter {
    /** When we halt stop the scheduler and notify anyone who cares.
      */
    def onContextHalted(): Unit = {
      // Runs regardless of discorporation.
      Logger.fine("Top level context complete.")
      runtime.removeRoot(PorcEExecution.this)
      PorcEExecution.this.synchronized {
        PorcEExecution.this.isDone = true
        PorcEExecution.this.notifyAll()
      }
    }
  }

  val t = new Terminator

  def scheduleProgram(prog: PorcEClosure): Unit = {
    // HACK: This is to force compilation on small programs.
    for(i <- 0 until 1) {
      runtime.schedule(new CounterSchedulable(c) {
        def run(): Unit = {
          try {
            prog.callFromRuntime(p, c, t)
          } finally {
            //
          }
        }
      })
    }
    c.halt()
  }
}
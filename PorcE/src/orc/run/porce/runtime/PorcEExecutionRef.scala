package orc.run.porce.runtime

import com.oracle.truffle.api.utilities.CyclicAssumption
import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.nodes.InvalidAssumptionException

// TODO: Remove this. It is only needed if we want to be able to run the same PorcE AST in multiple executions and that is probably not needed.
class PorcEExecutionHolder(exec: PorcEExecution) {
  holder =>

  val assumption = new CyclicAssumption("Execution not changed")

  private[this] var execution = exec

  def setExecution(e: PorcEExecution): Boolean = {
    if (execution.isDone) {
      synchronized {
        assumption.invalidate()
        execution = e
      }
      true
    } else {
      false
    }
  }

  private class PorcEExecutionRefImpl extends PorcEExecutionRef {
    @CompilerDirectives.CompilationFinal
    private[this] var executionCache = holder.execution
    @CompilerDirectives.CompilationFinal
    private[this] var assumptionCache = holder.assumption.getAssumption()

    def get() = {
      try {
        assumptionCache.check()
        executionCache
      } catch {
        case _: InvalidAssumptionException =>
          CompilerDirectives.transferToInterpreterAndInvalidate()
          holder.synchronized {
            executionCache = holder.execution
            assumptionCache = holder.assumption.getAssumption()
          }
          executionCache
      }
    }
  }

  def newRef(): PorcEExecutionRef = {
    new PorcEExecutionRefImpl
  }
}

abstract class PorcEExecutionRef {
  def get(): PorcEExecution
}


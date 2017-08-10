package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicBoolean

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

trait PorcERuntimeOperations {
  this: PorcERuntime =>

  @TruffleBoundary
  def spawn(c: Counter, computation: PorcEClosure): Unit = {
    scheduleOrCall(c, () => computation.callFromRuntime())
    // TODO: PERFORMANCE: Allowing run here is a critical optimization. Even with a small depth limit (32) this can give a factor of 6.
  }
    
  final def schedulePublish(p: PorcEClosure, c: Counter, v: Array[AnyRef]) = {
    scheduleOrCall(c, () => { 
      p.callFromRuntimeVarArgs(v)
    })
  }
}

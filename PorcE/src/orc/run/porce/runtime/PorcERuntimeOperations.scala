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
      
  @TruffleBoundary(throwsControlFlowException = true)
  def resolve(p: PorcEClosure, c: Counter, t: Terminator, vs: Array[AnyRef]) = {
    t.checkLive()
    val resolver = new Resolve(vs) with Terminatable {
      // The flag saying if we have already halted.
      protected var halted = new AtomicBoolean(false) 
      // TODO: PERFORMANCE: Ideally we could delay this add until we know we will actually be blocking.
      t.addChild(this)
      
      def done(): Unit = {
        if (halted.compareAndSet(false, true)) {
          t.removeChild(this)
          // Token: Passed on.
          schedulePublish(p, c, Array())
        }
      }
      
      def kill(): Unit = {
        if (halted.compareAndSet(false, true)) {
          c.haltToken()
        }
      }
    }
    if (resolver()) {
      // The resolver is instantly complete. So we handle the callback here.
      t.removeChild(resolver)
      p.callFromRuntime()
    }
  }
    
  final def schedulePublish(p: PorcEClosure, c: Counter, v: Array[AnyRef]) = {
    scheduleOrCall(c, () => { 
      p.callFromRuntimeVarArgs(v)
    })
  }
}

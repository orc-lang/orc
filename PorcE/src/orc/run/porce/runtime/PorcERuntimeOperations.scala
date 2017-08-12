package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicBoolean

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

trait PorcERuntimeOperations {
  this: PorcERuntime =>

  @TruffleBoundary @noinline
  def spawn(c: Counter, computation: PorcEClosure): Unit = {
    schedule(CallClosureSchedulable(computation))
  }
}

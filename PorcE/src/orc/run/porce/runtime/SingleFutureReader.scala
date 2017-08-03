package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicBoolean
import orc.FutureReader

final class SingleFutureReader(p: PorcEClosure, c: Counter, t: Terminator, runtime: PorcERuntime) extends AtomicBoolean with FutureReader with Terminatable {
  // The value stored in the AtomicBoolean is a flag saying if we have already halted.
  
  t.addChild(this)
  
  def publish(v: AnyRef): Unit = {
    if (compareAndSet(false, true)) {
      t.removeChild(this)
      // Token: pass to p
      runtime.schedulePublish(p, c, Array(v))
    }
  }
  
  def halt(): Unit = {
    if (compareAndSet(false, true)) {
      t.removeChild(this)
      c.haltToken() // Token: from c.
    }
  }

  def kill(): Unit = {
    // This join has been killed
    halt()
  }
}
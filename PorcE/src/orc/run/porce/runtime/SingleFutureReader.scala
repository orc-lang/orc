package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicBoolean

import orc.FutureReader
import orc.run.porce.SimpleWorkStealingSchedulerWrapper

final class SingleFutureReader(p: PorcEClosure, c: Counter, t: Terminator, execution: PorcEExecution) extends AtomicBoolean with FutureReader with Terminatable {
  // The value stored in the AtomicBoolean is a flag saying if we have already halted.

  t.addChild(this)
  
  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)

  def publish(v: AnyRef): Unit = {
    if (compareAndSet(false, true)) {
      t.removeChild(this)
      /* ROOTNODE-STATISTICS
			p.body.getRootNode() match {
			  case n: PorcERootNode => n.incrementBindSingle()
  		  case _ => ()
			}
			*/
      val s = CallClosureSchedulable(p, v, execution)
      SimpleWorkStealingSchedulerWrapper.shareSchedulableID(s, this)
      // Token: pass to p
      execution.runtime.potentiallySchedule(s)
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

  override def toString() = {
    s"SingleFutureReader@${hashCode().formatted("%x")}(${get()})"
  }
}
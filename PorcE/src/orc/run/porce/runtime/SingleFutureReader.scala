package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicBoolean
import orc.FutureReader
import orc.run.porce.PorcERootNode
import orc.run.porce.SimpleWorkStealingSchedulerWrapper

final class SingleFutureReader(p: PorcEClosure, c: Counter, t: Terminator, execution: PorcEExecution) extends AtomicBoolean with FutureReader with Terminatable {
  // The value stored in the AtomicBoolean is a flag saying if we have already halted.

  t.addChild(this)
  
  val contID = SimpleWorkStealingSchedulerWrapper.newSchedulableID() 
  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, contID)

  def publish(v: AnyRef): Unit = {
    if (compareAndSet(false, true)) {
      t.removeChild(this)
			p.body.getRootNode() match {
			  case n: PorcERootNode => n.incrementBindSingle()
  		  case _ => ()
			}
      val s = CallClosureSchedulable(p, v, execution)
      s.id = contID
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
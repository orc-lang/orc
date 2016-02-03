package orc.run.tojava

import java.util.concurrent.atomic.AtomicInteger

/**
 * @author amp
 */
abstract class Counter {
  /** The number of executions that are either running or pending.
    *
    * This functions similarly to a reference count and this halts when count
    * reaches 0.
    */
  val count = new AtomicInteger(1)

  /** Decrement the count and check for overall halting.
    *
    * If we did halt call onContextHalted().
    */
  def halt(): Unit = {
    val n = count.decrementAndGet()
    assert(n >= 0, "Halt is not allowed on already stopped CounterContexts")
    //Logger.finest(s"Decr $n in $this")
    if (n == 0) {
      onContextHalted();
    }
  }

  /** Increment the count.
    */
  def prepareSpawn(): Unit = {
    val n = count.getAndIncrement()
    //Logger.finest(s"Incr $n in $this")
    assert(n > 0, "Spawning is not allowed once we go to zero count. No zombies allowed!!!")
  }

  /** Called when this whole context has halted.
    */
  def onContextHalted(): Unit
}

/**
 * @author amp
 */
final class CounterNested(parent: Counter, haltContinuation: Runnable) extends Counter {
  /** Called when this whole context has halted.
    */
  def onContextHalted(): Unit = {
    haltContinuation.run()
    parent.halt()
  }
}
package orc.run.tojava

import java.util.concurrent.atomic.AtomicInteger
import orc.run.Logger
import java.util.logging.Level

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
    //Logger.log(Level.FINEST, s"Decr $n in $this", new Exception)
    assert(n >= 0, s"Halt is not allowed on already stopped CounterContexts $this")
    if (n == 0) {
      onContextHalted();
    }
  }

  /** Increment the count.
    */
  def prepareSpawn(): Unit = {
    val n = count.getAndIncrement()
    //Logger.log(Level.FINEST, s"Incr $n in $this", new Exception)
    assert(n > 0, s"Spawning is not allowed once we go to zero count. No zombies allowed!!! $this")
  }

  /** Called when this whole context has halted.
    */
  def onContextHalted(): Unit
}

/**
 * @author amp
 */
final class CounterNested(parent: Counter, haltContinuation: Runnable) extends Counter {
  // Matched against: onContextHalted call to halt
  parent.prepareSpawn()
  
  /** Called when this whole context has halted.
    */
  def onContextHalted(): Unit = {
    try {
      haltContinuation.run()    
    } catch {
      case _: KilledException => ()
    }
    // Matched against: constructor call to prepareSpawn
    parent.halt()
  }
}
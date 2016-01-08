package orc.run.tojava

import java.util.function.Consumer
import java.util.function.Function
import orc.OrcRuntime
import orc.Schedulable
import orc.OrcEvent
import orc.Handle
import orc.error.OrcException
import scala.util.parsing.input.Position
import java.util.concurrent.atomic.AtomicInteger
import orc.run.Logger
import java.util.function.BiConsumer

abstract class Context {  
  val runtime: OrcRuntime
  
  def publish(v: AnyRef): Unit
  def halt(): Unit

  def spawn(f: Consumer[Context]): Unit = {
    runtime.schedule(new ContextSchedulableFunc(this, () => f.accept(this)))
  }
  
  def spawnFuture(f: Consumer[Context]): Future = {
    val fut = new Future(runtime)
    runtime.schedule(new ContextSchedulableFunc(this, () =>
      f.accept(new ContextBase(this) {
        override def publish(v: AnyRef): Unit = {
          fut.bind(v)
        }
      })
    ))
    fut
  }
  
  /** Setup for a spawn, but don't actually spawn anything.
   *  
   *  This is used in Future and possibly other places to prepare for a later execution.
   * 
   */
  def prepareSpawn(): Unit
  
  def checkLive(): Unit = {
    if(! isLive()) {
      throw KilledException.SINGLETON
    }
  }
  def isLive(): Boolean
}

/**
 * @author amp
 */
class ContextBase(val parent: Context) extends Context {
  import Context._
  
  val runtime: OrcRuntime = parent.runtime
  
  def publish(v: AnyRef): Unit = {
    parent.publish(v)
  }
  
  def halt(): Unit = {
    parent.halt()
  }
  
  /** Setup for a spawn, but don't actually spawn anything.
   *  
   *  This is used in Future and possibly other places to prepare for a later execution.
   * 
   */
  def prepareSpawn(): Unit = {
    parent.prepareSpawn()
  }

  def isLive(): Boolean = {
    parent.isLive()
  }
}

final class ContextHandle(p: Context, val callSitePosition: Position) extends ContextBase(p) with Handle {
  def notifyOrc(event: OrcEvent): Unit = ???
  def setQuiescent(): Unit = ???

  def !!(e: OrcException): Unit = ???

  def hasRight(rightName: String): Boolean = ???
}

final class BranchContext(p: Context, publishImpl: BiConsumer[Context, AnyRef]) extends ContextBase(p) {
  override def publish(v: AnyRef): Unit = {
    publishImpl.accept(this, v)
  }
}

trait ContextCounterSupport extends Context {
  val count = new AtomicInteger(1)
  
  override def halt(): Unit = {
    val n = count.decrementAndGet() 
    Logger.info(s"Decr $n")
    if(n <= 0) {
      onContextHalted();
    }
  }

  override def prepareSpawn(): Unit = {
    val n = count.getAndIncrement()
    Logger.info(s"Incr $n")
    assert(n > 0, "Spawning is not allowed once we go to zero count. No zombies allowed!!!")
  }
  
  override def isLive(): Boolean = {
    count.get() > 0
  }

  def onContextHalted(): Unit
}

final class CounterContext(p: Context, ctxHaltImpl: Consumer[Context]) extends ContextBase(p) with ContextCounterSupport {
  parent.prepareSpawn()
  
  def onContextHalted(): Unit = {
    ctxHaltImpl.accept(this)
    // Notify parent that everything here has halted.
    parent.halt()
  }
}

abstract class CounterContextBase(p: Context) extends ContextBase(p) with ContextCounterSupport {
  parent.prepareSpawn()
  
  def onContextHalted(): Unit = {
    // Notify parent that everything here has halted.
    parent.halt()
  }
}

final class RootContext(override val runtime: OrcRuntime) extends Context with ContextCounterSupport {
  override def publish(v: AnyRef): Unit = {
    println(v)
  }
  
  def onContextHalted(): Unit = {
    Logger.info("Top level context complete.")
    runtime.stopScheduler()
  }
}

object Context {
}



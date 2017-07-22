package orc.run.porce.runtime

import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.frame.VirtualFrame
import orc.run.porce.PorcEUnit
import orc.FutureReader
import orc.run.porce.Logger
import orc.FutureBound
import orc.FutureUnbound
import orc.FutureStopped
import java.util.concurrent.atomic.AtomicBoolean

trait PorcERuntimeOperations {
  this: PorcERuntime =>

  def spawn(c: Counter, computation: PorcEClosure): Unit = {
    scheduleOrCall(c, () => computation.callFromRuntime())
    // TODO: PERF: Allowing run here is a critical optimization. Even with a small depth limit (32) this can give a factor of 6.
  }
      
  def resolve(p: PorcEClosure, c: Counter, t: Terminator, vs: Array[AnyRef]) = {
    t.checkLive()
    val resolver = new Resolve(vs) with Terminatable {
      // The flag saying if we have already halted.
      protected var halted = new AtomicBoolean(false) 
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
    resolver()
  }
  
  /** Force a list of values: forcing futures and resolving closures.
    *
    * If vs contains a ForcableCallableBase it must have a correct and complete closedValues.
    */
  def force(p: PorcEClosure, c: Counter, t: Terminator, vs: Array[AnyRef]): Unit = {
    assert(vs.length > 0)
    vs.length match {
      case 1 => {
        forceSingle(p, c, t, vs)
      }
      case _ => {
        // Token: Pass to join.
        val joiner = new PCTJoin(p, c, t, vs, true)
        joiner()
      }
    }
  }

  def forceSingle(p: PorcEClosure, c: Counter, t: Terminator, vs: Array[AnyRef]): Unit = {
    assert(vs.length == 1)
    val v = vs(0)
    v match {
      case f: orc.Future => {
        f.get() match {
          case FutureBound(v) => {
            // TODO: This should use a call node somehow. Without that this cannot be inlined I think.
            p.callFromRuntime(v)
          }
          case FutureStopped => {
            // Do nothing p will never be called.
            c.haltToken()
          }
          case FutureUnbound => {
            f.read(new PCTFutureReader(p, c, t))
          }
        }
      }
      case _ => {
        schedulePublish(p, c, vs)
      }
    }
  }
  
  private final def schedulePublish(p: PorcEClosure, c: Counter, v: Array[AnyRef]) = {
    scheduleOrCall(c, () => { 
      p.callFromRuntimeVarArgs(v)
    })
  }

  private final class PCTJoin(p: PorcEClosure, c: Counter, t: Terminator, vs: Array[AnyRef], forceClosures: Boolean)
      extends Join(vs, forceClosures) with Terminatable {
    t.addChild(this)

    Logger.finer(s"Spawn for PCJoin $this (${vs.mkString(", ")})")

    def done(): Unit = {
      Logger.finer(s"Done for $this")
      t.removeChild(this)
      // Token: Pass to p.
      schedulePublish(p, c, values)
    }
    def halt(): Unit = {
      Logger.finer(s"Halt for $this")
      t.removeChild(this)
      // Token: Remove token passed in.
      c.haltToken()
    }

    def kill(): Unit = {
      if (halted.compareAndSet(false, true)) {
        // This join has been killed
        halt()
      }
    }

    /*
    override def toString(): String = {
      s"PCTJoin@${hashCode().formatted("%08x")}(${halted.get()}, $nUnbound, ${vs.toSeq}, ${values.toSeq})"
    }
    */
  }
  
  private final class PCTFutureReader(p: PorcEClosure, c: Counter, t: Terminator) extends FutureReader with Terminatable {
    // The flag saying if we have already halted.
    protected var halted = new AtomicBoolean(false)
    
    t.addChild(this)
    
    def publish(v: AnyRef): Unit = {
      if (halted.compareAndSet(false, true)) {
        t.removeChild(this)
        // Token: pass to p
        schedulePublish(p, c, Array(v))
      }
    }
    
    def halt(): Unit = {
      if (halted.compareAndSet(false, true)) {
        t.removeChild(this)
        c.haltToken() // Token: from c.
      }
    }

    def kill(): Unit = {
      // This join has been killed
      halt()
    }
  }
}
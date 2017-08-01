package orc.run.porce.runtime

import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.frame.VirtualFrame
import orc.run.porce.PorcEUnit
import orc.FutureReader
import orc.run.porce.Logger
import java.util.concurrent.atomic.AtomicBoolean
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

trait PorcERuntimeOperations {
  this: PorcERuntime =>

  @TruffleBoundary(allowInlining = true)
  def spawn(c: Counter, computation: PorcEClosure): Unit = {
    scheduleOrCall(c, () => computation.callFromRuntime())
    // TODO: PERFORMANCE: Allowing run here is a critical optimization. Even with a small depth limit (32) this can give a factor of 6.
  }
      
  @TruffleBoundary(allowInlining = true)
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
  
  /*
  /** Force a list of values: forcing futures and resolving closures.
    *
    * If vs contains a ForcableCallableBase it must have a correct and complete closedValues.
    */
  @TruffleBoundary(allowInlining = true)
  def force(p: PorcEClosure, c: Counter, t: Terminator, vs: Array[AnyRef]): Unit = {
    assert(vs.length > 0)
    vs.length match {
      case 1 => {
        forceSingle(p, c, t, vs)
      }
      case _ => {
        // Token: Pass to join.
        val joiner = new PCTJoin(p, c, t, vs)
        joiner() match {
          case null => {
            // The joiner has taken over.
          }
          case Join.HaltSentinel => {
            // The join halted immediately.
            // Do nothing p will never be called.
            t.removeChild(joiner)
            c.haltToken()
          }
          case vs => {
            t.removeChild(joiner)
            p.callFromRuntimeVarArgs(vs)
          }
        }
      }
    }
  }

  def forceSingle(p: PorcEClosure, c: Counter, t: Terminator, vs: Array[AnyRef]): Unit = {
    assert(vs.length == 1)
    val v = vs(0)
    v match {
      case f: orc.Future => {
        f.get() match {
          case orc.FutureState.Bound(v) => {
            // TODO: This should use a call node somehow. Without that this cannot be inlined I think.
            p.callFromRuntime(v)
          }
          case orc.FutureState.Stopped => {
            // Do nothing p will never be called.
            c.haltToken()
          }
          case orc.FutureState.Unbound => {
            f.read(new PCTFutureReader(p, c, t))
          }
        }
      }
      case _ => {
        p.callFromRuntime(v)
      }
    }
  }
  */
  
  final def schedulePublish(p: PorcEClosure, c: Counter, v: Array[AnyRef]) = {
    scheduleOrCall(c, () => { 
      p.callFromRuntimeVarArgs(v)
    })
  }

  /*
  */
}

final class PCTFutureReader(p: PorcEClosure, c: Counter, t: Terminator, runtime: PorcERuntime) extends FutureReader with Terminatable {
  // The flag saying if we have already halted.
  protected var halted = new AtomicBoolean(false)
  
  t.addChild(this)
  
  def publish(v: AnyRef): Unit = {
    if (halted.compareAndSet(false, true)) {
      t.removeChild(this)
      // Token: pass to p
      runtime.schedulePublish(p, c, Array(v))
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

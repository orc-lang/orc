package orc.run.porce.runtime

import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.api.frame.VirtualFrame
import orc.run.porce.PorcEUnit
import orc.FutureReader
import orc.run.porce.Logger
import orc.FutureBound
import orc.FutureUnbound
import orc.FutureStopped

trait PorcERuntimeOperations {
  this: PorcERuntime =>

   def spawn(c: Counter, computation: PorcEClosure): Unit = {
    scheduleOrCall(c, () => computation.callFromRuntime())
    // PERF: Allowing run here is a critical optimization. Even with a small depth limit (32) this can give a factor of 6.
  }
    
  def spawnBindFuture(fut: Future, c: Counter, computation: PorcEClosure) = {
    scheduleOrCall(c, () => {
      val p = Utilities.PorcEClosure(new RootNode(null) {
        def execute(frame: VirtualFrame): Object = {
          // Skip the first argument since it is our captured value array.
          val v = frame.getArguments()(1)
          fut.bind(v)
          PorcEUnit.SINGLETON
        }      
      })
      val newC = new CounterNestedBase(c) {
        // Initial count matches: halt() in finally below.
        override def onContextHalted(): Unit = {
          //Logger.fine(s"Bound future $fut = stop")
          if (!isDiscorporated) {
            fut.stop()
          }
          super.onContextHalted()
        }
      }
      try {
        schedule(new CounterSchedulableFunc(newC, () => computation.callFromRuntime(p, newC)))
      } finally {
        // Matches: the starting count of newC
        newC.halt()
      }
    })    
  }
  
  def resolve(p: PorcEClosure, c: Counter, t: Terminator, vs: Array[AnyRef]) = {
    t.checkLive()
    // Matches: halt in done() below
    c.prepareSpawn()
    new Resolve(vs) {
      def done(): Unit = {
        schedulePublishAndHalt(p, c, Array())
      }
    }
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
        // Matches: call to halt in done and halt in PCJoin
        c.prepareSpawn()
        new PCJoin(p, c, vs, true)
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
          }
          case FutureUnbound => {
            c.prepareSpawn() // Matches: c.halt below or halt in schedulePublishAndHalt.
            f.read(new FutureReader() {
              def publish(v: AnyRef): Unit = {
                schedulePublishAndHalt(p, c, Array(v))
              }
              def halt(): Unit = {
                c.halt() // Matches: c.prepareSpawn above.
              }
            })
          }
        }
      }
      case _ => {
        schedulePublish(p, c, vs)
      }
    }
  }
  
  private final def schedulePublishAndHalt(p: PorcEClosure, c: Counter, v: Array[AnyRef]) = {
    scheduleOrCall(c, () => {
      try {
        p.callFromRuntimeVarArgs(v)
      } finally {
        // Matches: Call to prepareSpawn in constructor
        c.halt()
      }
    })
  }
  
  private final def schedulePublish(p: PorcEClosure, c: Counter, v: Array[AnyRef]) = {
    scheduleOrCall(c, () => { 
      p.callFromRuntimeVarArgs(v)
    })
  }

  private final class PCJoin(p: PorcEClosure, c: Counter, vs: Array[AnyRef], forceClosures: Boolean) extends Join(vs, forceClosures) {
    Logger.finer(s"Spawn for PCJoin $this (${vs.mkString(", ")})")

    def done(): Unit = {
      Logger.finer(s"Done for PCJoin $this (${values.mkString(", ")})")
      schedulePublishAndHalt(p, c, values)
    }
    def halt(): Unit = {
      Logger.finer(s"Halt for PCJoin $this (${values.mkString(", ")})")
      c.halt()
    }
  }
}
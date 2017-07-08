package orc.run.core

import orc.FutureReadHandle
import orc.FutureState
import orc.FutureUnbound
import orc.FutureStopped
import orc.FutureBound

class TokenFutureReadHandle(val caller: Token) extends FutureReadHandle with Blocker {
  private var value: FutureState = FutureUnbound

  private def runtime = caller.runtime
  
  private def schedule() = {
    runtime.schedule(caller)
  }
  
  def check(t: Blockable): Unit = {
    value match {
      case FutureUnbound => { 
        throw new AssertionError("Spurious check of call handle. " + this) 
      }
      case FutureBound(v) => {
        t.awakeTerminalValue(v)
      }
      case FutureStopped => { 
        t.awakeStop()
      }
    }
  }

  def halt(): Unit = {
    value = FutureStopped
    schedule()
  }

  def publish(v: AnyRef): Unit = {
    value = FutureBound(v)
    schedule()
  }
}
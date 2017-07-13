package orc.run.core

import orc.FutureReader
import orc.FutureState
import orc.FutureUnbound
import orc.FutureStopped
import orc.FutureBound

class TokenFutureReader(val caller: Token) extends FutureReader with Blocker {
  protected var value: FutureState = FutureUnbound

  protected def runtime = caller.runtime
  
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

class TokenFuturePublisher(caller: Token) extends TokenFutureReader(caller) {
  override def check(t: Blockable): Unit = {
    value match {
      case FutureUnbound => { 
        throw new AssertionError("Spurious check of call handle. " + this) 
      }
      case FutureBound(v) => {
        t.awakeNonterminalValue(v)
        t.halt()
      }
      case FutureStopped => { 
        t.halt()
      }
    }
  }
}

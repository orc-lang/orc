//
// TokenFutureReader.scala -- Scala classes TokenFutureReader and TokenFuturePublisher
// Project OrcScala
//
// Created by amp on Jul 7, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

import orc.{ FutureReader, FutureState }

class TokenFutureReader(val caller: Token) extends FutureReader with Blocker {
  protected var value: FutureState = FutureState.Unbound

  protected def runtime = caller.runtime
  
  private def schedule() = {
    runtime.schedule(caller)
  }
  
  def check(t: Blockable): Unit = {
    value match {
      case FutureState.Unbound => { 
        throw new AssertionError("Spurious check of call handle. " + this) 
      }
      case FutureState.Bound(v) => {
        t.awakeTerminalValue(v)
      }
      case FutureState.Stopped => { 
        t.awakeStop()
      }
    }
  }

  def halt(): Unit = {
    value = FutureState.Stopped
    schedule()
  }

  def publish(v: AnyRef): Unit = {
    value = FutureState.Bound(v)
    schedule()
  }
}

class TokenFuturePublisher(caller: Token) extends TokenFutureReader(caller) {
  override def check(t: Blockable): Unit = {
    value match {
      case FutureState.Unbound => { 
        throw new AssertionError("Spurious check of call handle. " + this) 
      }
      case FutureState.Bound(v) => {
        t.awakeNonterminalValue(v)
        t.halt()
      }
      case FutureState.Stopped => { 
        t.halt()
      }
    }
  }
}

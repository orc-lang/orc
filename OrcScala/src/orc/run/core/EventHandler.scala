package orc.run.core

import orc.CaughtEvent
import orc.OrcEvent
import orc.run.Logger
import orc.error.runtime.TokenError
import java.util.logging.Level

trait EventHandler {
  protected var eventHandler: OrcEvent => Unit

  def installHandler(newHandler: PartialFunction[OrcEvent, Unit]) = {
    val oldHandler = eventHandler
    eventHandler = { e => if (newHandler isDefinedAt e) newHandler(e) else oldHandler(e) }
  }

  def notifyOrc(event: OrcEvent) {
    try {
      eventHandler(event)
    } catch {
      case e: InterruptedException => throw e
      case e: Throwable => { Logger.log(Level.SEVERE, "Event handler abnormal termination", e); throw e }
    }
  }
}

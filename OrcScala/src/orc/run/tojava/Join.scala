package orc.run.tojava

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** @author amp
  */
abstract class Join(inValues: Array[AnyRef]) {
  join =>
  var nUnbound = new AtomicInteger(inValues.size)
  val values = Array.ofDim[AnyRef](inValues.size)
  var halted = new AtomicBoolean(false)

  final class JoinElement(i: Int) extends Blockable {
    def publish(v: AnyRef): Unit = {
      if (!halted.get()) {
        values(i) = v
        nUnbound.decrementAndGet()
        join.checkComplete()
      }
    }
    def halt(): Unit = {
      if (halted.compareAndSet(false, true)) {
        join.halt()
      }
    }

    def prepareSpawn(): Unit = {}
  }

  var nNonFutures = 0
  for ((v, i) <- inValues.zipWithIndex) v match {
    case f: Future => {
      f.forceIn(new JoinElement(i))
    }
    case _ => {
      values(i) = v
      nNonFutures += 1
    }
  }
  nUnbound.getAndAdd(-nNonFutures)
  checkComplete()

  final def checkComplete(): Unit = {
    assert(nUnbound.get >= 0)
    if (nUnbound.get == 0) {
      done()
    }
  }

  def done(): Unit

  def halt(): Unit
}


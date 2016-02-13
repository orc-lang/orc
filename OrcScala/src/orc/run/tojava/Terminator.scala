package orc.run.tojava

import java.util.concurrent.atomic.AtomicBoolean

trait Terminatable {
  def kill(): Unit
}

/** The a termination tracker.
  *
  * @author amp
  */
class Terminator extends Terminatable {
  private[this] var isLiveFlag = true
  // TODO: children can theoretically grow without bound. We need to actually remove the children when they are gone.
  private[this] var children: List[Terminatable] = Nil

  def addChild(child: Terminatable) = synchronized {
    if (!isLive()) {
      child.kill()
      checkLive()
    }
    children ::= child
  }

  /** Check that this context is live and throw KilledException if it is not.
    */
  def checkLive(): Unit = {
    if (!isLive()) {
      throw KilledException.SINGLETON
    }
  }

  /** Return true if this context is still live (has not been killed or halted
    * naturally).
    */
  def isLive() = {
    // TODO: Double checking the flag might be worth it.
    synchronized { isLiveFlag }
  }

  /** Kill the expressions under this terminator.
    *
    * This will throw KilledException if the terminator has already been killed otherwise it will just return to allow handling.
    */
  def kill(): Unit = {
    val res = synchronized {
      if (isLiveFlag) {
        val t = children
        isLiveFlag = false
        children = null // Cause errors if anything is added later.
        Some(t)
      } else {
        None
      }
    }
    res match {
      case Some(cs) =>
        for (c <- cs) {
          try {
            c.kill()
          } catch {
            case _: KilledException => {}
          }
        }
      case None =>
        throw KilledException.SINGLETON
    }
  }
}

/** The a termination tracker.
  *
  * @author amp
  */
final class TerminatorNested(parent: Terminator) extends Terminator {
  parent.addChild(this)
}
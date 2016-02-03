package orc.run.tojava

import java.util.concurrent.atomic.AtomicBoolean

/** The a termination tracker.
  *
  * @author amp
  */
class Terminator {
  private[this] var isLiveFlag = true
  private[this] var children: List[Terminator] = Nil

  def addChild(child: Terminator) = synchronized {
    checkLive()
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
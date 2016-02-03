package orc.run.tojava

/** Blockable represents any object that can block on something and then
  * handle the result.
  *
  * @author amp
  */
trait Blockable {
  /** Unblock with a publication.
    *
    * This does not imply halting: instead halt will be called as well if an
    * explicit halt is needed.
    */
  def publish(v: AnyRef): Unit

  /** Halt the Blockable.
    *
    * This can occur without or without a publication. Every call to this must
    * match a call to prepareSpawn().
    */
  def halt(): Unit

  /** Setup for a later execution possibly in another thread.
    *
    * This doesn't actually spawn anything, but prepares for waiting for
    * execution in another thread of control, such as spawning a new execution.
    *
    * Every call to this must be matched by a later call to halt().
    */
  def prepareSpawn(): Unit
}

final class PCBlockable(p: Continuation, c: Counter) extends Blockable {
  def publish(v: AnyRef): Unit = p.call(v)
  def halt(): Unit = c.halt()
  def prepareSpawn(): Unit = c.prepareSpawn()
}
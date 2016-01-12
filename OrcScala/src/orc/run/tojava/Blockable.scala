package orc.run.tojava

/**
 * @author amp
 */
trait Blockable {
  def publish(v: AnyRef): Unit
  def halt(): Unit

  /** Setup for a spawn, but don't actually spawn anything.
    *
    * This is used in Future and possibly other places to prepare for a later execution.
    *
    */
  def prepareSpawn(): Unit
}
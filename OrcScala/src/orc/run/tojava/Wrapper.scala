package orc.run.tojava

/** @author amp
  */
trait Wrapper {
  def underlying: AnyRef
}

object Wrapper {
  def unwrap(v: AnyRef) = v match {
    case w: Wrapper => w.underlying
    case _ => v
  }
}

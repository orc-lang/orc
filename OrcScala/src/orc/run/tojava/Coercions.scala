package orc.run.tojava

import orc.values.sites.DirectSite
import orc.run.Logger

/**
 * @author amp
 */
object Coercions {
  /** Coerce any value to a Callable.
    *
    * If it is a Callable, just use that. Otherwise assume it is a Java object
    * we would like to call and wrap it for a runtime based invocation.
    */
  def coerceToCallable(v: AnyRef): Callable = {
    v match {
      case c: Callable => c
      // TODO: We may want to optimize cases like records and site calls.
      //case s: Site => new SiteCallable(s)
      case f: Future => {
        Logger.warning(s"Generating FutureCallable. The translation should force all targets before call.\n$f")
        new FutureCallable(f)
      }
      case v => new RuntimeCallable(v)
    }
  }
  
  def coerceToDirectCallable(v: AnyRef): DirectCallable = {
    v match {
      case c: DirectCallable => c
      // TODO: We may want to optimize cases like records and site calls.
      //case s: Site => new SiteCallable(s)
      case f: Future => throw new IllegalArgumentException(s"Cannot direct call a future: $f")
      case v: DirectSite => new RuntimeDirectCallable(v)
    }
  }
  
  def coerceToContinuation(v: AnyRef): Continuation = v.asInstanceOf[Continuation]
  def coerceToTerminator(v: AnyRef): Terminator = v.asInstanceOf[Terminator]
  def coerceToCounter(v: AnyRef): Counter = v.asInstanceOf[Counter]
}
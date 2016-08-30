package orc.run.tojava

import orc.values.sites.DirectSite
import orc.run.Logger
import orc.values.sites.Site

/**
 * @author amp
 */
object Coercions {
  /** Coerce any value to a Callable.
    *
    * If it is a Callable, just use that. Otherwise assume it is a Java object
    * we would like to call and wrap it for a runtime based invocation.
    */
  def coerceSiteToCallable(v: AnyRef): Callable = {
    v match {
      case c: Callable => c
      // TODO: We may want to optimize cases like records and site calls.
      //case s: Site => new SiteCallable(s)
      case v => new RuntimeCallable(v)
    }
  }
  
  def coerceSiteToDirectCallable(v: AnyRef): DirectCallable = {
    v match {
      case c: DirectCallable => c
      case f: Future => throw new IllegalArgumentException(s"Cannot direct call a future: $f")
      case v: DirectSite => new RuntimeDirectCallable(v)
      case s: Site => throw new IllegalArgumentException(s"Non-direct site found when direct site expected.")
    }
  }
  
  def isInstanceOfDef(v: AnyRef): Boolean = {
    // This is kind of opaque, but all defs are always forcable and no other callable is.
    v.isInstanceOf[ForcableCallableBase]
  }
  
  //def coerceToContinuation(v: AnyRef): Continuation = v.asInstanceOf[Continuation]
  //def coerceToTerminator(v: AnyRef): Terminator = v.asInstanceOf[Terminator]
  //def coerceToCounter(v: AnyRef): Counter = v.asInstanceOf[Counter]
}
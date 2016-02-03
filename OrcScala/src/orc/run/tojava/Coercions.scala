package orc.run.tojava

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
      case v => new RuntimeCallable(v)
    }
  }
  
  def coerceToDirectCallable(v: AnyRef): AnyRef = ???
  def coerceToContinuation(v: AnyRef): Continuation = v.asInstanceOf[Continuation]
  def coerceToTerminator(v: AnyRef): Terminator = v.asInstanceOf[Terminator]
  def coerceToCounter(v: AnyRef): Counter = v.asInstanceOf[Counter]
}
package orc.run.tojava

import java.util.concurrent.atomic.AtomicBoolean

import orc.error.compiletime.SiteResolutionException
import orc.values.sites.{ Effects, Site }

// TODO: It might be good to have calls randomly schedule themselves to unroll the stack.
/** An object that can be called directly from within the tojava runtime.
  *
  * @author amp
  */
trait Callable {
  // TODO: This cannot track call positions. That probably should be possible.
  // However I'm not at all sure how that should work since it needs to also allow stack building for def calls and eventually orc site calls.
  /** Call this object with the given arguments. Publications will go into
    * ctx.
    *
    * This may schedule later execution and hence returning does not imply
    * halting. If this does schedule later execution then this will handle
    * the spawn on ctx correctly (prepareSpawn() and halt()).
    */
  def call(ctx: Context, args: Array[AnyRef])
}

/** A Callable implementation that uses ctx.runtime to handle the actual call.
  *
  * This uses the token interpreters site invocation code and hence uses
  * several shims to convert from one API to another.
  */
final class RuntimeCallable(s: AnyRef) extends Callable {
  def call(ctx: Context, args: Array[AnyRef]) = {
    // If this call could have effects, check for kills.
    s match {
      case s: Site if s.effects == Effects.None => {}
      case _ => ctx.checkLive()
    }

    // Prepare to spawn because the invoked site might do that.
    ctx.prepareSpawn();
    // Matched to: halt in ContextHandle or below in Join subclass.

    if (args.length == 0) {
      ctx.runtime.invoke(new ContextHandle(ctx, null), s, Nil)
    } else {
      // TODO: Optimized version for single argument
      new Join(args) {
        // A debug flag to make sure halt/done are called no more than once.
        lazy val called = new AtomicBoolean(false)

        /** If the join halts then the context should be halted as well.
          */
        def halt(): Unit = {
          assert(called.compareAndSet(false, true), "halt()/done() may only be called once.")
          ctx.halt() // Matched to: prepareSpawn above
        }
        /** If the join completes (so all values are bound) we perform the
          * invocation.
          */
        def done(): Unit = {
          assert(called.compareAndSet(false, true), "halt()/done() may only be called once.")
          ctx.runtime.invoke(new ContextHandle(ctx, null), s, values.toList)
        }
      }
    }
  }
}

object Callable {
  /** Resolve an Orc Site name to a Callable.
    */
  def resolveOrcSite(n: String): Callable = {
    try {
      new RuntimeCallable(orc.values.sites.OrcSiteForm.resolve(n))
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }

  /** Resolve an Java Site name to a Callable.
    */
  def resolveJavaSite(n: String): Callable = {
    try {
      new RuntimeCallable(orc.values.sites.JavaSiteForm.resolve(n))
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }

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
}

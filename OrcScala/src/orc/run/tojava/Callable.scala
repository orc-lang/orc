package orc.run.tojava

import orc.values.sites.Site
import orc.error.compiletime.SiteResolutionException
import orc.values.sites.Effects
import orc.error.runtime.UncallableValueException
import orc.values.OrcRecord

// TODO: It might be good to have calls randomly schedule themselves to unroll the stack.
/**
 * @author amp
 */
trait Callable {
  // TODO: This cannot track call positions. That probably should be possible.
  // However I'm not at all sure how that should work since it needs to also allow stack building for def calls and eventually orc site calls.
  def call(ctx: Context, args: Array[AnyRef])
}

final class RuntimeCallable(s: AnyRef) extends Callable {
  def call(ctx: Context, args: Array[AnyRef]) = {
    // If this call could have effects, check for kills.
    s match {
      case s: Site if s.effects == Effects.None => {}
      case _ => ctx.checkLive()
    }

    ctx.prepareSpawn();
    
    if (args.length == 0) {
      ctx.runtime.invoke(new ContextHandle(ctx, null), s, Nil)
    } else {
      // TODO: Optimized version for single argument
      new Join(args) {
        def halt(): Unit = {
          ctx.halt()
        }
        def done(): Unit = {
          ctx.runtime.invoke(new ContextHandle(ctx, null), s, values.toList)
        }
      }
    }
  }  
}

object Callable {
  def resolveOrcSite(n: String): Callable = {
    try {
      new RuntimeCallable(orc.values.sites.OrcSiteForm.resolve(n))
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }
  def resolveJavaSite(n: String): Callable = {
    try {
      new RuntimeCallable(orc.values.sites.JavaSiteForm.resolve(n))
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }
  
  def coerceToCallable(v: AnyRef): Callable = {
    v match {
      case c: Callable => c
      // TODO: We may want to optimize cases like records and site calls.
      //case s: Site => new SiteCallable(s)
      case v => new RuntimeCallable(v)
    }
  }
}

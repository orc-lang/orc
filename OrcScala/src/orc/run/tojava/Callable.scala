package orc.run.tojava

import orc.values.sites.Site
import orc.error.compiletime.SiteResolutionException
import orc.values.sites.Effects
import orc.error.runtime.UncallableValueException
import orc.values.OrcRecord

/**
 * @author amp
 */
trait Callable {
  // TODO: This cannot track call positions. That probably should be possible.
  // However I'm not at all sure how that should work since it needs to also allow stack building for def calls and eventually orc site calls.
  def call(ctx: Context, args: Array[AnyRef])
}

final class SiteCallable(s: Site) extends Callable {
  def call(ctx: Context, args: Array[AnyRef]) = {
    // If this call could have effects check for kills.
    if (s.effects != Effects.None)
      ctx.checkLive()

    ctx.prepareSpawn();

    new Join(args) {
      def halt(): Unit = {
        ctx.halt()
      }
      def done(): Unit = {
        // TODO: This should be optimized for cases where the args require less conversion.
        s.call(values.toList, new ContextHandle(ctx, null))
      }
    }
  }  
}

object Callable {
  def resolveOrcSite(n: String): Callable = {
    try {
      new SiteCallable(orc.values.sites.OrcSiteForm.resolve(n))
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }
  def resolveJavaSite(n: String): Callable = {
    try {
      new SiteCallable(orc.values.sites.JavaSiteForm.resolve(n))
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }
  
  def coerceToCallable(v: AnyRef): Callable = {
    v match {
      case c: Callable => c
      case s: Site => new SiteCallable(s)
      case r: OrcRecord => ???
      case _ => throw new UncallableValueException(v)
    }
  }
}

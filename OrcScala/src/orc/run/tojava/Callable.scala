package orc.run.tojava

import orc.values.sites.Site
import orc.error.compiletime.SiteResolutionException

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
    ctx.prepareSpawn();
    // TODO: This should be optimized for cases where the args require less conversion.
    s.call(args.toList, new ContextHandle(ctx, null))
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
}

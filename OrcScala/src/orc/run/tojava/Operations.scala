package orc.run.tojava

import orc.values.OrcRecord
import orc.values.Field
import orc.values.sites.Site

/**
 * @author amp
 */
object Operations {
  def force(ctx: Context, v: AnyRef) = {
    v match {
      case f: Future => f.forceIn(ctx)
      case _ => ctx.publish(v)
    }
  }
  
  def getField(ctx: Context, v: AnyRef, f: Field) = {
    v match {
      case r: OrcRecord => {
        r.entries.get(f.field) match {
          case Some(w) => ctx.publish(w)
          case None => {}
        }
      }
      case _ => {
        val c = Callable.coerceToCallable(v)
        c.call(ctx, Array[AnyRef](f))
      }
    }
  }
}
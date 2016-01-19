package orc.run.tojava

import orc.values.{ Field, OrcRecord }

/** Utility operations for Java 8 code generated from Orc code.
  * @author amp
  */
object Operations {
  /** Force a value if it is a future.
    */
  def force(ctx: Context, v: AnyRef) = {
    v match {
      case f: Future =>
        // TODO: I don't think checkLive is required here and it might be more expensive than just forcing.
        //ctx.checkLive() //Disabled to allow more inlining by the JIT.
        f.forceIn(ctx)
      case _ => ctx.publish(v)
    }
  }

  /** Get a field of an object if possible.
    *
    * This may result in a site call and hence a spawn. However all the halts
    * and stuff are handled internally. However it does mean that this
    * returning does not imply this has halted.
    */
  def getField(ctx: Context, v: AnyRef, f: Field) = {
    v match {
      case r: OrcRecord => {
        // This just optimizes the record case.
        r.entries.get(f.field) match {
          case Some(w) => ctx.publish(w)
          case None => {}
        }
      }
      case _ => {
        // Use the old style call with field to get the value.
        val c = Callable.coerceToCallable(v)
        c.call(ctx, Array[AnyRef](f))
      }
    }
  }
}
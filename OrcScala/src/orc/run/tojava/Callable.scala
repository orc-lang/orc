package orc.run.tojava

import java.util.concurrent.atomic.AtomicBoolean
import orc.error.compiletime.SiteResolutionException
import orc.values.sites.{ Effects, Site }
import orc.values.sites.SiteMetadata
import orc.values.sites.DirectSite
import orc.error.OrcException
import orc.error.runtime.JavaException
import orc.CaughtEvent
import orc.values.OrcRecord
import orc.values.sites.HasFields
import orc.values.Field
import orc.run.Logger

trait Continuation {
  def call(v: AnyRef)
}

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
  def call(execution: Execution, p: Continuation, c: Counter, t: Terminator, args: Array[AnyRef])
}

/** An object that can be called directly from within the tojava runtime.
  *
  * @author amp
  */
trait DirectCallable {
  // TODO: This cannot track call positions. That probably should be possible.
  // However I'm not at all sure how that should work since it needs to also allow stack building for def calls and eventually orc site calls.
  /** Call this object with the given arguments. Publications will go into
    * ctx.
    *
    * This may schedule later execution and hence returning does not imply
    * halting. If this does schedule later execution then this will handle
    * the spawn on ctx correctly (prepareSpawn() and halt()).
    */
  def directcall(execution: Execution, args: Array[AnyRef]): AnyRef
}

trait ForcableCallableBase {
  /** The set of values that this closure holds references too.
    *
    * This must be complete in the sense that no value in this list
    * references another possible future that is not in this list.
    */
  val closedValues: Array[AnyRef]
}

final class ForcableCallable(val closedValues: Array[AnyRef], impl: Callable) extends Callable with ForcableCallableBase {
  def call(execution: Execution, p: Continuation, c: Counter, t: Terminator, args: Array[AnyRef]): Unit = 
    impl.call(execution, p, c, t, args)
}
final class ForcableDirectCallable(val closedValues: Array[AnyRef], impl: DirectCallable) extends DirectCallable with ForcableCallableBase {
  def directcall(execution: Execution, args: Array[AnyRef]): AnyRef =
    impl.directcall(execution, args)
}

/** A Callable implementation that uses ctx.runtime to handle the actual call.
  *
  * This uses the token interpreters site invocation code and hence uses
  * several shims to convert from one API to another.
  */
class RuntimeCallable(val underlying: AnyRef) extends Callable with Wrapper {
  private lazy val site = Callable.findSite(underlying)
  final def call(execution: Execution, p: Continuation, c: Counter, t: Terminator, args: Array[AnyRef]) = {
    // If this call could have effects, check for kills.
    site match {
      case s: SiteMetadata if s.effects == Effects.None => {}
      case _ => t.checkLive()
    }

    // Prepare to spawn because the invoked site might do that.
    c.prepareSpawn()
    // Matched to: halt in PCTHandle or below in Join subclass.

    if (args.length == 0) {
      execution.invoke(new PCTHandle(execution, p, c, t, null), site, Nil)
    } else {
      // TODO: Optimized version for single argument
      new Join(args) {
        // A debug flag to make sure halt/done are called no more than once.
        lazy val called = new AtomicBoolean(false)

        /** If the join halts then the context should be halted as well.
          */
        def halt(): Unit = {
          assert(called.compareAndSet(false, true), "halt()/done() may only be called once.")
          c.halt() // Matched to: prepareSpawn above
        }
        /** If the join completes (so all values are bound) we perform the
          * invocation.
          */
        def done(): Unit = {
          assert(called.compareAndSet(false, true), "halt()/done() may only be called once.")
          execution.invoke(new PCTHandle(execution, p, c, t, null), site, values.toList)
        }
      }
    }
  }
  
  override def toString: String = s"${getClass.getName}($underlying)"
}

/** A Callable implementation that uses ctx.runtime to handle the actual call.
  *
  * This uses the token interpreters site invocation code and hence uses
  * several shims to convert from one API to another.
  */
final class RuntimeDirectCallable(u: DirectSite) extends RuntimeCallable(u) with DirectCallable with Wrapper {
  private lazy val site = Callable.findSite(u)
  def directcall(execution: Execution, args: Array[AnyRef]) = {
    Logger.fine(s"Direct calling: $u(${args.mkString(", ")})")
    try {
      val v = try {
        site.calldirect(args.toList)
      } catch {
        case e: InterruptedException => 
          throw e
        case e: ExceptionHaltException if e.getCause() != null => 
          execution.notifyOrc(CaughtEvent(e.getCause()))
          throw HaltException.SINGLETON
        case e: HaltException => 
          throw e
        case e: Exception => 
          execution.notifyOrc(CaughtEvent(e))
          throw HaltException.SINGLETON
      }
      Logger.fine(s"Direct call returned successfully: $u(${args.mkString(", ")}) = $v")
      v
    } catch {
      case e: Exception =>
        Logger.fine(s"Direct call halted: $u(${args.mkString(", ")}) -> $e")
        throw e
    }
  }
}

// FIXME: I think this is a bit of a hack. I think more low lever operation could be better than generating these intermediate operations at runtime.
class FutureCallable(val underlying: Future) extends Callable {
  final def call(execution: Execution, p: Continuation, c: Counter, t: Terminator, args: Array[AnyRef]) = {
    underlying.forceIn(new PCBlockable(new Continuation {
      def call(v: AnyRef) = {
        Coercions.coerceToCallable(v).call(execution, p, c, t, args)
      }
    }, c))
  }
  
  override def toString: String = s"${getClass.getName}($underlying)"
}


object Callable {
  def findSite(s: AnyRef): AnyRef = s match {
    case r: HasFields if r.hasField(Field("apply")) => 
      r.getField(Field("apply")) match {
      case applySite: Site => findSite(applySite)
      case _ => s
    }
    case _ => s
  }
  def findSite(s: DirectSite): DirectSite = s match {
    case r: HasFields if r.hasField(Field("apply")) => r.getField(Field("apply")) match {
      case applySite: DirectSite => findSite(applySite)
      case _ => s
    }
    case _ => s
  }
  
  /** Resolve an Orc Site name to a Callable.
    */
  def resolveOrcSite(n: String): Callable = {
    try {
      val s = orc.values.sites.OrcSiteForm.resolve(n)
      s match {
        case s: DirectSite => new RuntimeDirectCallable(s)
        case _ => new RuntimeCallable(s)
      }
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }

  /** Resolve an Java Site name to a Callable.
    */
  def resolveJavaSite(n: String): Callable = {
    try {
      val s = orc.values.sites.JavaSiteForm.resolve(n)
      s match {
        case s: DirectSite => new RuntimeDirectCallable(s)
        case _ => new RuntimeCallable(s)
      }
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }
}

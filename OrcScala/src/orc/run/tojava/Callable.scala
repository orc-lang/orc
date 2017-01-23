//
// Callable.scala -- Scala trait Callable and related traits
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
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
import orc.values.Field
import orc.run.Logger
import orc.OrcEvent
import scala.util.parsing.input.Position
import orc.Handle
import orc.OrcRuntime
import orc.compile.parse.OrcSourceRange
import orc.ExecutionRoot
import orc.values.HasMembers
import orc.run.core.BoundValue
import orc.run.core.BoundReadable

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

/** A wrapper around ToJava runtime state which interacts correctly with the Orc site API.
  *
  */
final class PCTHandle(val execution: Execution, p: Continuation, c: Counter, t: Terminator) extends Handle with Terminatable {
  val halted = new AtomicBoolean(false)

  t.addChild(this)

  def publishNonterminal(v: AnyRef): Unit = {
    execution.stageOrRun(new CounterSchedulableFunc(c, () => p.call(v)))
  }

  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override def publish(v: AnyRef) = {
    if (halted.compareAndSet(false, true)) {
      // TODO: It should be possible to pass the count we have on to the schedulable. It would save two atomic updates per pub.
      execution.stageOrRun(new CounterSchedulableFunc(c, () => p.call(v)))
      c.halt()
      // Matched to: Every invocation is required to be proceeded by a
      //             prepareSpawn since it might spawn.
    }
  }

  def kill(): Unit = halt

  def halt: Unit = {
    if (halted.compareAndSet(false, true)) {
      c.halt()
      // Matched to: Every invocation is required to be proceeded by a
      //             prepareSpawn since it might spawn.
    }
  }

  def notifyOrc(event: OrcEvent): Unit = {
    execution.notifyOrc(event)
  }

  // TODO: Support VTime
  def setQuiescent(): Unit = {}

  def !!(e: OrcException): Unit = {
    notifyOrc(CaughtEvent(e))
    halt
  }

  // TODO: Support rights.
  def hasRight(rightName: String): Boolean = false

  def isLive: Boolean = {
    t.isLive()
  }

  def callSitePosition: Option[OrcSourceRange] = None

  val runtime: OrcRuntime = execution.runtime.runtime

  def discorporate(): Unit = {
    // TODO: Add support for discorporation to Porc and the ToJava backend.
    ???
  }
}

/** A Callable implementation that uses ctx.runtime to handle the actual call.
  *
  * This uses the token interpreters site invocation code and hence uses
  * several shims to convert from one API to another.
  */
sealed class RuntimeCallable(val underlying: AnyRef) extends Callable with Wrapper {
  lazy val site = Callable.findSite(underlying)
  final def call(execution: Execution, p: Continuation, c: Counter, t: Terminator, args: Array[AnyRef]) = {
    // If this call could have effects, check for kills.
    site match {
      case s: SiteMetadata if s.effects == Effects.None => {}
      case _ => t.checkLive()
    }

    // Prepare to spawn because the invoked site might do that.
    c.prepareSpawn()
    // Matched to: halt in PCTHandle.
    execution.setStage()
    try {
      execution.invoke(new PCTHandle(execution, p, c, t), site, args)
    } finally {
      execution.flushStage()
    }
  }

  override def toString: String = s"${getClass.getName}($underlying)"
}

/** A Callable implementation that uses ctx.runtime to handle the actual call.
  *
  * This uses the token interpreters site invocation code and hence uses
  * several shims to convert from one API to another.
  */
final class RuntimeDirectCallable(override val underlying: DirectSite) extends RuntimeCallable(underlying) with DirectCallable with Wrapper {
  override lazy val site = Callable.findSite(underlying)
  def directcall(execution: Execution, args: Array[AnyRef]) = {
    Logger.fine(s"Direct calling: $underlying(${args.mkString(", ")})")
    try {
      val v = try {
        execution.setStage()
        try {
          site.calldirect(args)
        } finally {
          execution.flushStage()
        }
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
      Logger.fine(s"Direct call returned successfully: $underlying(${args.mkString(", ")}) = $v")
      v
    } catch {
      case e: Exception =>
        Logger.fine(s"Direct call halted: $underlying(${args.mkString(", ")}) -> $e")
        throw e
    }
  }
}

object Callable {
  def findSite(s: AnyRef): AnyRef = s match {
    case r: HasMembers if r.hasMember(Field("apply")) =>
      r.getMember(Field("apply")) match {
        case BoundReadable(r) => ??? // Handle readables
        case BoundValue(applySite: Site) => findSite(applySite)
        case _ => s
      }
    case _ => s
  }
  def findSite(s: DirectSite): DirectSite = s match {
    case r: HasMembers if r.hasMember(Field("apply")) =>
      r.getMember(Field("apply")) match {
        case BoundReadable(r) => ??? // Handle readables
        case BoundValue(applySite: DirectSite) => findSite(applySite)
        case _ => s
      }
    case _ => s
  }

  /** Resolve an Orc Site name to a Callable.
    */
  def resolveOrcSite(n: String): RuntimeCallable = {
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

  /** Resolve an Orc Site name to a Callable.
    */
  def resolveOrcDirectSite(n: String): RuntimeDirectCallable = {
    try {
      val s = orc.values.sites.OrcSiteForm.resolve(n)
      s match {
        case s: DirectSite => new RuntimeDirectCallable(s)
        case _ => throw new AssertionError("resolveOrcDirectSite should never be called with a non-direct site.")
      }
    } catch {
      case e: SiteResolutionException =>
        throw new Error(e)
    }
  }

  /** Resolve an Java Site name to a Callable.
    */
  def resolveJavaSite(n: String): RuntimeCallable = {
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

  def rethrowDirectCallException(execution: Execution, e: Exception): Nothing = e match {
    case e: InterruptedException =>
      throw e
    case e: ExceptionHaltException if e.getCause() != null =>
      execution.notifyOrc(CaughtEvent(e.getCause()))
      throw HaltException.SINGLETON
    case e: HaltException =>
      throw e
    case _ =>
      execution.notifyOrc(CaughtEvent(e))
      throw HaltException.SINGLETON
  }
}

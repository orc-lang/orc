//
// OrcRuntimeInterface.scala -- Interfaces for Orc runtime
// Project OrcScala
//
// Created by amp on July 12, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import orc.ast.oil.nameless.Expression
import orc.compile.parse.OrcSourceRange
import orc.error.OrcException
import orc.error.runtime.ExecutionException
import orc.values.Signal

// TODO: OrcRuntimeProvides is a duplicate of the abilities provided by Backend's Runtime. This should probably be removed and reorganized as a utility class for backends.
/** The interface from a caller to an Orc runtime
  */
trait OrcRuntimeProvides {
  @throws[ExecutionException]
  def run(e: Expression, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions): Unit
  def stop(): Unit
}

/** The interface from an Orc runtime to its environment
  */
trait OrcRuntimeRequires extends InvocationBehavior
/** An interface for objects which can receive future results.
  *
  * Elements of Orc runtimes should implement this interface to support
  * handling future results. Non-orc code can also implement this interface
  * to interact with Orc futures when implementing external routine methods.
  */
trait FutureReader {
  /** Called if the future is bound to a value.
    *
    * The value may not be another Future.
    *
    * `publish` must execute quickly as the time this takes to execute will delay
    * the execution of the binder of the future. The implementation may block on
    * locks if needed, but the lock latency should be as short as possible.
    * 
    * `publish` must be thread-safe.
    */
  def publish(v: AnyRef): Unit

  /** Called if the future is bound to stop.
    *
    * `halt` must execute quickly as the time this takes to execute will delay
    * the execution of the binder of the future. The implementation may block on
    * locks if needed, but the lock latency should be as short as possible.
    * 
    * `halt` must be thread-safe.
    */
  def halt(): Unit
}

/** The state of a Future.
  */
abstract sealed class FutureState()

object FutureState {
  /** The future is currently not resolved. */
  final case object Unbound extends FutureState()

  /** The future is resolved to stop. */
  final case object Stopped extends FutureState()

  /** The future is bound to a specific value. */
  final case class Bound(value: AnyRef) extends FutureState()
}

/** An interface for futures or future wrappers from other systems.
  *
  * This interface is directly implemented by Orc futures and should be
  * easy to implement using a wrapper on most future types. However, due
  * to the async callback based nature of this interface futures that only
  * provide polling or blocking interfaces will need a background thread or
  * other machinery to implement this interface. This is unavoidable.
  */
trait Future {
  /** Return the state of the future.
    */
  def get: FutureState

  /** Register to get the value of this future.
    *
    * reader methods may be called during the execution of this call (in this thread) or
    * called in another thread at any point after this call begins.
    */
  def read(reader: FutureReader): Unit
}

object StoppedFuture extends Future {
  def get = FutureState.Stopped
  def read(reader: FutureReader) = {
    reader.halt()
  }
}

case class BoundFuture(v: AnyRef) extends Future {
  def get = FutureState.Bound(v)
  def read(reader: FutureReader) = {
    reader.publish(v)
  }
}

/** The interface through which the environment response to site calls.
  *
  * Published values passed to publish and publishNonterminal may not be futures.
  */
trait CallContext {

  // TODO: Consider making this a separate API that is not core to the Orc JVM API.
  /** Submit an event to the Orc runtime.
    */
  def notifyOrc(event: OrcEvent): Unit

  // TODO: Replace with onidle API.
  /** Specify that the call is quiescent and will remain so until it halts or is killed.
    */
  def setQuiescent(): Unit

  /** Publish a value from this call without halting the call.
    */
  def publishNonterminal(v: AnyRef): Unit

  /** Publish a value from this call and halt the call.
    */
  def publish(v: AnyRef): Unit = {
    publishNonterminal(v)
    halt
  }

  @deprecated("Use publish(Signal) explicitly.", "3.0")
  def publish() { publish(Signal) }

  /** Halt this call without publishing a value.
    */
  def halt(): Unit

  @deprecated("Use halt(e).", "3.0")
  def !!(e: OrcException): Unit = halt(e)

  /** Halt this call without publishing a value, providing an exception which caused the halt.
    */
  def halt(e: OrcException): Unit

  /** Notify the runtime that the call will never publish again, but will not halt.
    */
  def discorporate(): Unit

  /** Provide a source position from which this call was made.
    *
    * Some runtimes may always return None.
    */
  def callSitePosition: Option[OrcSourceRange]

  /** Return true iff the caller has the right named.
    */
  def hasRight(rightName: String): Boolean

  /** Return true iff the call is still live (not killed).
    */
  def isLive: Boolean
}

/** An event reported by an Orc execution
  */
trait OrcEvent

case class PublishedEvent(value: AnyRef) extends OrcEvent
case object HaltedOrKilledEvent extends OrcEvent
case class CaughtEvent(e: Throwable) extends OrcEvent

/** An action for a few major events reported by an Orc execution.
  * This is an alternative to receiving <code>OrcEvents</code> for a client
  * with simple needs, or for Java code that cannot create Scala functions.
  */
class OrcEventAction {
  val asFunction: (OrcEvent => Unit) = _ match {
    case PublishedEvent(v) => published(v)
    case CaughtEvent(e) => caught(e)
    case HaltedOrKilledEvent => haltedOrKilled()
    case event => other(event)
  }

  def published(value: AnyRef) {}
  def caught(e: Throwable) {}
  def haltedOrKilled() {}

  @throws[Exception]
  def other(event: OrcEvent) {}
}

trait Schedulable extends Runnable {
  /** A schedulable unit may declare itself nonblocking;
    * the scheduler may exploit this information.
    * It is assumed by default that a schedulable unit might block.
    */
  val nonblocking: Boolean = false
  
  /** The priority of this schedulable.
    * 
    * This should not change after it is first requested, so this should
    * be implemented as val or lazy val.
    */
  def priority: Int = 0

  /** Invoked just before this schedulable unit is scheduled or staged.
    * It is run in the thread that made the enqueueing call.
    */
  def onSchedule() {}

  /** Invoked after this schedulable unit has been run by the scheduler and
    * has completed (successfully or not). It is run in the same thread that
    * executed the unit.
    */
  def onComplete() {}
}

/** The type of root executions in Orc runtimes.
  *
  * Currently empty and only provided here to avoid a reference to the
  * implementation in the API.
  */
trait ExecutionRoot {
}

/** An Orc runtime
  */
trait OrcRuntime extends OrcRuntimeProvides with OrcRuntimeRequires {

  def startScheduler(options: OrcExecutionOptions): Unit

  def schedule(t: Schedulable): Unit

  def stage(ts: List[Schedulable]): Unit

  // Schedule function is overloaded for convenience
  def stage(t: Schedulable) { stage(List(t)) }
  def stage(t: Schedulable, u: Schedulable) { stage(List(t, u)) }

  def stopScheduler(): Unit

  def removeRoot(exec: ExecutionRoot): Unit
}

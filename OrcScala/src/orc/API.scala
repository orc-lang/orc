//
// API.scala -- Interfaces for Orc compiler and runtime
// Project OrcScala
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import java.io.{ File, IOException }

import orc.ast.oil.nameless.Expression
import orc.compile.parse.{ OrcInputContext, OrcSourceRange }
import orc.error.OrcException
import orc.error.compiletime.CompileLogger
import orc.error.runtime.ExecutionException
import orc.progress.ProgressMonitor
import orc.values.Signal
import scala.util.parsing.input.Position
import orc.compile.CompilerFlagValue
import orc.error.runtime.HaltException
import orc.values.Field
import orc.error.runtime.{ TypeNoSuchMemberException, UncallableTypeException, NoSuchMemberException, UncallableValueException, TypeDoesNotHaveMembersException }
import orc.error.runtime.DoesNotHaveMembersException

// TODO: This file is huge and covers many unrelated things. Split into multiple sub API with limited relationships. Also it's not clear where to put utility implementations/bases for these APIs.

/** The interface from a caller to the Orc compiler
  */
trait OrcCompilerProvides[+E] {
  @throws(classOf[IOException])
  def apply(source: OrcInputContext, options: OrcCompilationOptions, compileLogger: CompileLogger, progress: ProgressMonitor): E
}

/** The interface from the Orc compiler to its environment
  */
trait OrcCompilerRequires {
  @throws(classOf[IOException])
  def openInclude(includeFileName: String, relativeTo: OrcInputContext, options: OrcCompilationOptions): OrcInputContext
  @throws(classOf[ClassNotFoundException])
  def loadClass(className: String): Class[_]
}

/** An Orc compiler
  */
trait OrcCompiler[+E] extends OrcCompilerProvides[E] with OrcCompilerRequires

/** The interface from a caller to an Orc runtime
  */
trait OrcRuntimeProvides {
  // TODO: Remove the expression argument on run. It is tied to the token interpreter since other interperters do not use Nameless.
  @throws[ExecutionException]
  def run(e: Expression, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions): Unit
  def stop(): Unit
}

/** The interface from an Orc runtime to its environment
  */
trait OrcRuntimeRequires extends InvocationBehavior

/** An action class implementing invocation for specific target and argument types.
  *
  * The fundemental difference between an Invoker and Accessor is that an accessor can return a future
  * for later forcing, where as an Invoker allows blocking during the call itself. These APIs are
  * mutually encodable, however this encoding would have a significant performance cost.
  * 
  * Invoker.canInvoke must only depend on immutable information in targets and the invoker. This means
  * canInvoke(v) will always return the same value for a specific value v. Similarly, if getInvoker(v) returns 
  * invoker, then invoker.canInvoke(v) must always be true.
  */
trait Invoker {
  /** Return true if InvocationBehavior#getInvoker would return an equivalent
    * instance for these argument types. Equivalent means that for these values
    * invoke would behave the same.
    *
    * This should be as fast as possible. Returning false erroneously is allowed,
    * but may dramatically effect performance on some backends.
    */
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean

  /** Invoke target with the given arguments.
    *
    * If canInvoke(target, arguments) would return false than the behavior of
    * this call is undefined.
    *
    * This call may still throw UncallableValueException even if canInvoke returns true.
    * This could occur for sites which have mutable values which may stop being callable.
    */
  @throws[UncallableValueException]
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit
}

/** Implement direct invocation for calls with do not block and do not need runtime access.
  *
  */
trait DirectInvoker extends Invoker {
  /** Invoke target with the given arguments are returns it's single publication or throws HaltException if
    * the invocation does not publish.
    *
    * This call may not block on external events, but may use locks as needed as long as the locks will
    * be available with relatively low-latency. Any delay in this call may delay the execution of unrelated
    * tasks or threads.
    * 
    * This call may still throw UncallableValueException even if canInvoke returns true.
    * This could occur for sites which have mutable values which may stop being callable.
    *
    * The returned value may not be a future.
    */
  @throws[HaltException]
  @throws[UncallableValueException]
  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef
}

/** Type of error sentinels returned by InvocationBehavior.getInvoker.
  *
  * These invokers must NEVER be cached.
  */
trait ErrorInvoker extends Invoker

/** A accessor sentinal representing the fact that unknownMember does not exist on the given value.
	*/
case class UncallableValueInvoker(target: AnyRef) extends ErrorInvoker {
  @throws[UncallableValueException]
  def invoke(h: Handle, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    throw new UncallableValueException(target)
  }

  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    this.target == target
  }
}

/** An action class implementing field extraction for a specific target type and field.
  *
  * The fundemental difference between an Invoker and Accessor is that an accessor can return a future
  * for later forcing, where as an Invoker allows blocking during the call itself. These APIs are
  * mutually encodable, however this encoding would have a significant performance cost.
  * 
  * Accessor.canGet must only depend on immutable information in targets and the accessor. This means
  * canGet(v) will always return the same value for a specific value v. Similarly, if getAccessor(v) returns 
  * accessor, then accessor.canGet(v) must always be true.
  */
trait Accessor {
  /** Return true if InvocationBehavior#getAccessor would return an equivalent
    * instance for this target type. Equivalent means that for these values get
    * would behave the same.
    *
    * This should be as fast as possible. Returning false erroniously is allowed,
    * but may dramatically effect performance on some backends.
    */
  def canGet(target: AnyRef): Boolean

  /** Extract the value of the field from target.
    *
    * The returned value may be a future. The caller must check the future and force
    * it when appropriate.
    *
    * If canGet(target) would return false than the behavior of this call is
    * undefined.
    *
    * This call may still throw NoSuchMemberException even if canGet returns true.
    * This occures for types with runtime variable sets of fields meaning that while
    * this is the correct accessor there is no field available.
    */
  @throws[NoSuchMemberException]
  @throws[DoesNotHaveMembersException]
  def get(target: AnyRef): AnyRef
}

/** Type of error sentinals returned by InvocationBehavior.getAccessor.
  *
  * These accessors must NEVER be cached.
  */
trait ErrorAccessor extends Accessor

/** A accessor sentinal representing the fact that unknownMember does not exist on the given value.
	*/
case class NoSuchMemberAccessor(target: AnyRef, unknownMember: String) extends ErrorAccessor {
  @throws[NoSuchMemberException]
  def get(target: AnyRef): AnyRef = {
    throw new NoSuchMemberException(target, unknownMember)
  }

  def canGet(target: AnyRef): Boolean = {
    this.target == target
  }
}

/** A accessor sentinal representing the fact that the value does not have members.
	*/
case class DoesNotHaveMembersAccessor(target: AnyRef) extends ErrorAccessor {
  @throws[DoesNotHaveMembersException]
  def get(target: AnyRef): AnyRef = {
    throw new DoesNotHaveMembersException(target)
  }

  def canGet(target: AnyRef): Boolean = {
    this.target == target
  }
}

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
    * onBound must execute quickly as the time this takes to execute will delay
    * the execution of the binder of the future. The implementation may block on
    * locks if needed, but the lock latency should be as short as possible.
    */
  def publish(v: AnyRef): Unit

  /** Called if the future is bound to stop.
    *
    * onHalted must execute quickly as the time this takes to execute will delay
    * the execution of the binder of the future. The implementation may block on
    * locks if needed, but the lock latency should be as short as possible.
    */
  def halt(): Unit
}

/** The state of a Future.
  */
sealed abstract class FutureState

/** The future is currently not resolved.
  */
final case object FutureUnbound extends FutureState

/** The future is resolved to stop.
  */
final case object FutureStopped extends FutureState

/** The future is bound to a specific value.
  */
final case class FutureBound(value: AnyRef) extends FutureState

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
  def get(): FutureState

  /** Register to get the value of this future.
    *
    * reader methods may be called during the execution of this call (in this thread) or
    * called in another thread at any point after this call begins.
    */
  def read(reader: FutureReader): Unit
}

object StoppedFuture extends Future {
  def get() = FutureStopped
  def read(reader: FutureReader) = {
    reader.halt()
  }
}

case class BoundFuture(v: AnyRef) extends Future {
  def get() = FutureBound(v)
  def read(reader: FutureReader) = {
    reader.publish(v)
  }
}

/** Define invocation behaviors for a runtime
  */
trait InvocationBehavior {
  /** Get an invoker for a specific target type and argment types.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Invoker or DirectInvoker for the given values or an 
    * 			  instance of InvokerError if there is no invoker.
    * 
    * @see UncallableValueInvoker
    */
  def getInvoker(target: AnyRef, arguments: Array[AnyRef]): Invoker

  /** Get an accessor which extracts a given field value from a target.
    *
    * This method is slow and the results should be cached if possible.
    *
    * @return An Accessor for the given classes or an 
    * 			  instance of AccessorError if there is no accessor.
    * 
    * @see NoSuchMemberAccessor, DoesNotHaveMembersAccessor
    */
  def getAccessor(target: AnyRef, field: Field): Accessor
}

/** The interface through which the environment response to site calls.
  *
  * Published values passed to publish and publishNonterminal may not be futures.
  */
trait Handle {

  // TODO: Consider making this a seperate API that is not core to the Orc JVM API.
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

/** Options for Orc compilation and execution.
  *
  * @author jthywiss
  */
trait OrcOptions extends OrcCompilationOptions with OrcExecutionOptions

trait OrcCommonOptions extends Serializable {
  def filename: String
  def filename_=(newVal: String)
  def classPath: java.util.List[String]
  def classPath_=(newVal: java.util.List[String])
  def logLevel: String
  def logLevel_=(newVal: String)
  def xmlLogFile: String
  def xmlLogFile_=(newVal: String)
  def backend: BackendType
  def backend_=(newVal: BackendType)
}

trait OrcCompilationOptions extends OrcCommonOptions {
  def usePrelude: Boolean
  def usePrelude_=(newVal: Boolean)
  def includePath: java.util.List[String]
  def includePath_=(newVal: java.util.List[String])
  def additionalIncludes: java.util.List[String]
  def additionalIncludes_=(newVal: java.util.List[String])
  def typecheck: Boolean
  def typecheck_=(newVal: Boolean)
  def disableRecursionCheck: Boolean
  def disableRecursionCheck_=(newVal: Boolean)
  def echoOil: Boolean
  def echoOil_=(newVal: Boolean)
  def echoIR: Int
  def echoIR_=(newVal: Int)
  def oilOutputFile: Option[File]
  def oilOutputFile_=(newVal: Option[File])
  def compileOnly: Boolean
  def compileOnly_=(newVal: Boolean)
  def runOil: Boolean
  def runOil_=(newVal: Boolean)

  def optimizationLevel: Int
  def optimizationLevel_=(newVal: Int)
  def optimizationOptions: java.util.List[String]
  def optimizationOptions_=(v: java.util.List[String])

  def optimizationFlags: Map[String, CompilerFlagValue]
}

trait OrcExecutionOptions extends OrcCommonOptions {
  def showJavaStackTrace: Boolean
  def showJavaStackTrace_=(newVal: Boolean)
  def disableTailCallOpt: Boolean
  def disableTailCallOpt_=(newVal: Boolean)
  def stackSize: Int
  def stackSize_=(newVal: Int)
  def maxTokens: Int
  def maxTokens_=(newVal: Int)
  def maxSiteThreads: Int
  def maxSiteThreads_=(newVal: Int)
  def hasRight(rightName: String): Boolean
  def setRight(rightName: String, newVal: Boolean)
}

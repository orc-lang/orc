//
// InvocationInterceptor.scala -- Scala trait DistributedInvocationInterceptor
// Project PorcE
//
// Created by jthywiss on Aug 15, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import java.util.concurrent.atomic.AtomicLong

import scala.util.control.NonFatal

import orc.{ CaughtEvent, Future }
import orc.FutureState.Bound
import orc.Schedulable
import orc.run.porce.runtime.{ CallClosureSchedulable, InvocationInterceptor, MaterializedCPSCallContext, PorcEClosure }

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

/** Intercept external calls from a DOrcExecution, and possibly migrate them to another Location.
  *
  * @author jthywiss
  * @author amp
  */
trait DistributedInvocationInterceptor extends InvocationInterceptor {
  execution: DOrcExecution =>

  // TODO: Optimize shouldInterceptInvocation to have the initial checks partially evaluated.
  // Currently there is no partial evaluation for simplicity, but it may be useful to have some initial
  // checks inlined. It may also be desirable to convert ShouldInterceptInvocation into
  // a Truffle node so that it can have true per site specialization.
  // TODO: PERFORMANCE: This would probably gain a lot by specializing on the number of arguments. That will probably require a simpler structure for the loops.

  /** Remove the PorcE context from the arg list, leaving the actual external call args */
  @inline
  private def cleanUpArgumentList(arguments: Array[AnyRef]): Array[AnyRef] = arguments.drop(3)

  @noinline
  @TruffleBoundary(allowInlining = true)
  override def shouldInterceptInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    def derefAnyBoundLocalFuture(value: AnyRef): AnyRef = {
      value match {
        case _: RemoteFutureRef => value
        case f: Future => f.get match {
          case Bound(bv) => bv
          case _ => value
        }
        case _ => value
      }
    }
    def safeToString(v: AnyRef): String = if (v == null) "null" else try v.toString() catch { case NonFatal(_) | _: LinkageError => s"${orc.util.GetScalaTypeName(v)}@${System.identityHashCode(v)}" }

    //Logger.Invoke.entering(getClass.getName, "shouldInterceptInvocation", Seq(orc.util.GetScalaTypeName(target), target) ++ arguments)
    Logger.Invoke.finest("class names: " + orc.util.GetScalaTypeName(target) + arguments.map(orc.util.GetScalaTypeName(_)).mkString("(", ",", ")"))
    //Logger.Invoke.finest("isInstanceOf[RemoteRef]: " + target.isInstanceOf[RemoteRef] + arguments.map(_.isInstanceOf[RemoteRef]).mkString("(", ",", ")"))

    val needsOverride = execution.callLocationMayNeedOverride(target, cleanUpArgumentList(arguments))
    Logger.Invoke.finest("needsOverride=" + needsOverride + " for types " + orc.util.GetScalaTypeName(target) + arguments.map(orc.util.GetScalaTypeName(_)).mkString("(", ",", ")"))
    val result = if (needsOverride.isDefined) {
      needsOverride.get
    } else if (/*FIXME:HACK*/target.isInstanceOf[PorcEClosure] && target.toString().startsWith("ᑅSubAstValueSetDef")) {
      /* Attempt to prospectively migrate to Sub-AST value set */
      /* Look up current locations, and find their intersection */
      val intersectLocs = arguments.map(derefAnyBoundLocalFuture(_)).map(execution.currentLocations(_)).fold(execution.currentLocations(target))({ _ & _ })
      Logger.Invoke.finest(s"prospective migrate: ${safeToString(target)}(${arguments.map(safeToString(_)).mkString(",")}): intersection of current locations=$intersectLocs")
      /* Prospective migrate found a location to move to */
      intersectLocs.nonEmpty && !(intersectLocs contains execution.runtime.here)
    } else {
      //FIXME: lists, refs, cells, etc. can accept RemoteRefs
      /* If everything's local, then stay here */
      !execution.isLocal(target) || arguments.exists(!execution.isLocal(_))
    }
    Logger.Invoke.exiting(getClass.getName, "shouldInterceptInvocation", "Returning " + result + " for types " + orc.util.GetScalaTypeName(target) + arguments.map(orc.util.GetScalaTypeName(_)).mkString("(", ",", ")"))
    result
  }

  override def invokeIntercepted(callContext: MaterializedCPSCallContext, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    Logger.Invoke.entering(getClass.getName, "invokeIntercepted", Seq(orc.util.GetScalaTypeName(target), target) ++ arguments)

    /* Case 1: If there's a call location override, use it */
    val clo = execution.callLocationOverride(target, arguments)
    val candidateDestinations = if (clo.nonEmpty) {
      Logger.Invoke.finest(s"siteCall: $target(${arguments.mkString(",")}): callLocationOverride=$clo")
      clo
    } else {
      /* Look up current locations, and find their intersection */
      val intersectLocs = arguments.map(execution.currentLocations(_)).fold(execution.currentLocations(target))({ _ & _ })
      require(!(intersectLocs contains execution.runtime.here), s"intersectLocs contains here on call: $target(${arguments.mkString(",")}")
      Logger.Invoke.finest(s"siteCall: $target(${arguments.mkString(",")}): intersection of current locations=$intersectLocs")

      if (intersectLocs.nonEmpty) {
        /* Case 2: If the intersection of current locations is non-empty, use it */
        intersectLocs
      } else {
        /* Look up permitted locations, and find their intersection */
        val intersectPermittedLocs = (arguments map execution.permittedLocations).fold(execution.permittedLocations(target)) { _ & _ }
        if (intersectPermittedLocs.nonEmpty) {
          /* Case 3: If the intersection of permitted locations is non-empty, use it */
          intersectPermittedLocs
        } else {
          /* Case 4: No permitted location, fail */
          val nla = new NoLocationAvailable((target +: arguments.toSeq).map(v => (v, execution.currentLocations(v).map(_.runtimeId))))
          execution.notifyOrc(CaughtEvent(nla))
          throw nla
        }
      }
    }
    Logger.Invoke.finest(s"siteCall: $target(${arguments.mkString(",")}): candidateDestinations=$candidateDestinations")
    val destination = execution.selectLocationForCall(candidateDestinations)
    Logger.Invoke.finest(s"siteCall: $target(${arguments.mkString(",")}): selected location for call: $destination")
    sendCall(callContext, target, arguments, destination)
  }

  /* Since we don't have token IDs in PorcE: */
  private val callCorrelationCounter = new AtomicLong(execution.followerExecutionNum.toLong << 32)

  def sendCall(callContext: MaterializedCPSCallContext, callTarget: AnyRef, callArguments: Array[AnyRef], destination: PeerLocation): Unit = {
    Logger.Invoke.entering(getClass.getName, "sendCall", Seq(callContext, callTarget) ++ callArguments :+ destination)

    val distributedCounter = execution.getDistributedCounterForCounter(callContext.c)
    // Token: Give our token back to the local representation
    val credit = distributedCounter.convertToken()
    // Credit: Get credit for the token to send elsewhere.
    assert(credit > 0)

    val terminatorProxyId = execution.makeProxyWithinTerminator(callContext.t, (terminatorProxyId) => execution.sendKilled(destination, terminatorProxyId)())

    val marshaledTarget = execution.marshalValue(destination)(callTarget)
    val marshaledArgs = callArguments map { execution.marshalValue(destination)(_) }

    val callMemento = new CallMemento(
      callContext,
      counterId = distributedCounter.id,
      credit = credit,
      terminatorProxyId = terminatorProxyId,
      target = marshaledTarget,
      arguments = marshaledArgs)

    val callCorrelationId = callCorrelationCounter.getAndIncrement()
    Tracer.traceCallSend(callCorrelationId, execution.runtime.here, destination)
    // Credit: The credit from above is passed in the message.
    destination.sendInContext(execution)(MigrateCallCmd(execution.executionId, callCorrelationId, callMemento))
  }

  def receiveCall(origin: PeerLocation, callCorrelationId: Long, callMemento: CallMemento): Unit = {
    val distributedCounter = execution.getDistributedCounterForId(callMemento.counterId)
    // Credit: Give credit from message to local representation
    distributedCounter.activate(callMemento.credit)
    // Token: Get token from local representation

    val proxyTerminator = execution.makeProxyTerminatorFor(callMemento.terminatorProxyId, origin)

    val unmarshaledTarget = execution.unmarshalValue(origin)(callMemento.target)
    val unmarshaledArgs = callMemento.arguments map { execution.unmarshalValue(origin)(_) }

    val callInvoker = new Schedulable {
      override def toString: String = s"execution.invokeCallTarget(${callMemento.callSiteId}, ${callMemento.publicationContinuation}, ${distributedCounter.counter}, ${proxyTerminator}, ${unmarshaledTarget}(${unmarshaledArgs.mkString(", ")}))"
      def run(): Unit = {
        // Token: Pass local token to the invocation.
        execution.invokeCallTarget(callMemento.callSiteId, callMemento.publicationContinuation, distributedCounter.counter, proxyTerminator, unmarshaledTarget, unmarshaledArgs)
      }
    }
    /* invokeCallTarget will add an appropriate child to the proxyTerminator */

    Tracer.traceCallReceive(callCorrelationId, origin, execution.runtime.here)

    Logger.Downcall.fine(s"Scheduling $callInvoker")
    // Token: Pass the initial token of the proxy counter to the call.
    execution.runtime.schedule(callInvoker)
  }

  def sendPublish(destination: PeerLocation, proxyId: RemoteRef#RemoteRefId)(publicationContinuation: PorcEClosure, publishedValue: AnyRef): Unit = {
    Logger.Invoke.fine(f"sendPublish: publish by proxyId $proxyId%#x")
    Tracer.tracePublishSend(proxyId, execution.runtime.here, destination)

    val marshaledPubValue = execution.marshalValue(destination)(publishedValue)
    destination.sendInContext(execution)(PublishGroupCmd(execution.executionId, proxyId, PublishMemento(publicationContinuation, marshaledPubValue)))
  }

  def publishInGroup(origin: PeerLocation, groupMemberProxyId: RemoteRef#RemoteRefId, publication: PublishMemento): Unit = {
    Logger.Invoke.entering(getClass.getName, "publishInGroup", Seq(groupMemberProxyId.toString, publication))
    val g = execution.proxiedTerminatorMembers.get(groupMemberProxyId)
    if (g != null) {
      Tracer.tracePublishReceive(groupMemberProxyId, origin, execution.runtime.here)
      val unmarshaledPubValue = execution.unmarshalValue(origin)(publication.publishedValue)
      Logger.Downcall.fine(s"Scheduling CallClosureSchedulable(${publication.publicationContinuation}, $unmarshaledPubValue)")
      execution.runtime.schedule(CallClosureSchedulable(publication.publicationContinuation, unmarshaledPubValue, execution))
    } else {
      throw new AssertionError(f"Publish by unknown group member proxy $groupMemberProxyId%#x, value=${publication.publishedValue}")
    }
  }
}

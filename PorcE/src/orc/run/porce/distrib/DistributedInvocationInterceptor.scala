//
// InvocationInterceptor.scala -- Scala trait DistributedInvocationInterceptor
// Project PorcE
//
// Created by jthywiss on Aug 15, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import java.util.concurrent.atomic.AtomicLong

import orc.Schedulable
import orc.run.porce.runtime.{ CPSCallResponseHandler, CallClosureSchedulable, InvocationInterceptor, PorcEClosure }

/** Intercept external calls from a DOrcExecution, and possibly migrate them to another Location.
  *
  * @author jthywiss
  * @author amp
  */
trait DistributedInvocationInterceptor extends InvocationInterceptor {
  execution: DOrcExecution =>

  override def shouldInterceptInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    // TODO: PERFORMANCE: This would probably gain a lot by specializing on the number of arguments. That will probably require a simpler structure for the loops.
    if (target.isInstanceOf[RemoteRef] || arguments.exists(_.isInstanceOf[RemoteRef])) {
      true
    } else {
      val here = execution.runtime.here
      @inline
      def checkValue(v: AnyRef): Boolean = {
        v.isInstanceOf[LocationPolicy] && !currentLocations(v).contains(here)
      }
      checkValue(target) || arguments.exists(checkValue)
    }
  }

  override def invokeIntercepted(callHandler: CPSCallResponseHandler, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    def pickLocation(ls: Set[PeerLocation]) = ls.head

    //Logger.Invoke.entering(getClass.getName, "invokeIntercepted", Seq(target.getClass.getName, target, arguments))

    // TODO: If this every turns out to be a performance issue I suspect a bloom-filter-optimized set would help.
    val intersectLocs = (arguments map currentLocations).fold(currentLocations(target)) { _ & _ }
    require(!(intersectLocs contains runtime.here))
    Logger.Invoke.finest(s"siteCall($target, $arguments): intersection of current locations=$intersectLocs")
    val candidateDestinations = {
      if (intersectLocs.nonEmpty) {
        intersectLocs
      } else {
        val intersectPermittedLocs = (arguments map permittedLocations).fold(permittedLocations(target)) { _ & _ }
        if (intersectPermittedLocs.nonEmpty) {
          intersectPermittedLocs
        } else {
          throw new NoLocationAvailable((target +: arguments.toSeq).map(v => (v, currentLocations(v).map(_.runtimeId))))
        }
      }
    }
    Logger.Invoke.finest(s"candidateDestinations=$candidateDestinations")
    val destination = pickLocation(candidateDestinations)
    sendCall(callHandler, target, arguments, destination)
  }

  /* Since we don't have token IDs in PorcE: */
  private val callCorrelationCounter = new AtomicLong(followerExecutionNum.toLong << 32)

  def sendCall(callContext: CPSCallResponseHandler, callTarget: AnyRef, callArguments: Array[AnyRef], destination: PeerLocation): Unit = {
    Logger.Invoke.fine(s"sendCall $callContext, $callTarget, $callArguments, $destination")

    val distributedCounter = getDistributedCounterForCounter(callContext.c)
    // Token: Give our token back to the local representation
    val credit = distributedCounter.convertToken()
    // Credit: Get credit for the token to send elsewhere.
    assert(credit > 0)

    val terminatorProxyId = makeProxyWithinTerminator(callContext.t, (terminatorProxyId) => sendKilled(destination, terminatorProxyId)())

    val marshaledTarget = execution.marshalValue(destination)(callTarget)
    val marshaledArgs = callArguments map { execution.marshalValue(destination)(_) }

    val callMemento = new CallMemento(callContext, 
        counterId = distributedCounter.id, 
        credit = credit, 
        terminatorProxyId = terminatorProxyId, 
        target = marshaledTarget, 
        arguments = marshaledArgs)

    val callCorrelationId = callCorrelationCounter.getAndIncrement()
    Tracer.traceCallSend(callCorrelationId, execution.runtime.here, destination)
    // Credit: The credit from above is passed in the message.
    destination.sendInContext(execution)(MigrateCallCmd(executionId, callCorrelationId, callMemento))
  }

  def receiveCall(origin: PeerLocation, callCorrelationId: Long, callMemento: CallMemento): Unit = {
    val distributedCounter = getDistributedCounterForId(callMemento.counterId)
    // Credit: Give credit from message to local representation
    distributedCounter.activate(callMemento.credit)
    // Token: Get token from local representation

    val proxyTerminator = makeProxyTerminatorFor(callMemento.terminatorProxyId, origin)

    val unmarshaledTarget = execution.unmarshalValue(origin)(callMemento.target)
    val unmarshaledArgs = callMemento.arguments map { execution.unmarshalValue(origin)(_) }

    val callInvoker = new Schedulable {
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
    val g = proxiedTerminatorMembers.get(groupMemberProxyId)
    if (g != null) {
      Tracer.tracePublishReceive(groupMemberProxyId, origin, execution.runtime.here)
      val unmarshaledPubValue = execution.unmarshalValue(origin)(publication.publishedValue)
      Logger.Downcall.fine(s"Scheduling CallClosureSchedulable(${publication.publicationContinuation}, $unmarshaledPubValue)")
      execution.runtime.schedule(CallClosureSchedulable(publication.publicationContinuation, unmarshaledPubValue))
    } else {
      throw new AssertionError(f"Publish by unknown group member proxy $groupMemberProxyId%#x, value=${publication.publishedValue}")
    }
  }
}

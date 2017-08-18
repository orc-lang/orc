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
import orc.run.porce.runtime.{ CPSCallResponseHandler, CallClosureSchedulable, InvocationInterceptor }
import orc.run.porce.runtime.PorcEClosure

/** Intercept external calls from a DOrcExecution, and possibly migrate them to another Location.
  *
  * @author jthywiss
  * @author amp
  */
trait DistributedInvocationInterceptor extends InvocationInterceptor {
  execution: DOrcExecution =>

  override def shouldInterceptInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    // WARNING: Contains return!!!

    // TODO: PERFORMANCE: This would probably gain a lot by specializing on the number of arguments. That will probably require a simpler structure for the loops.
    if (target.isInstanceOf[RemoteRef] || arguments.exists(_.isInstanceOf[RemoteRef])) {
      return true
    } else {
      val here = execution.runtime.here
      for (v <- arguments.view :+ target) {
        if (v.isInstanceOf[LocationPolicy] && !currentLocations(v).contains(here)) {
          return true
        }
      }

      return false
    }
  }

  override def invokeIntercepted(callHandler: CPSCallResponseHandler, target: AnyRef, arguments: Array[AnyRef]): Unit = {
    def pickLocation(ls: Set[PeerLocation]) = ls.head

    //Logger.entering(getClass.getName, "invokeIntercepted", Seq(target.getClass.getName, target, arguments))

    // TODO: If this every turns out to be a performance issue I suspect a bloom-filter-optimized set would help.
    val intersectLocs = (arguments map currentLocations).fold(currentLocations(target)) { _ & _ }
    require(!(intersectLocs contains runtime.here))
    Logger.finest(s"siteCall($target,$arguments): intersection of current locations=$intersectLocs")
    val candidateDestinations = {
      if (intersectLocs.nonEmpty) {
        intersectLocs
      } else {
        val intersectPermittedLocs = (arguments map permittedLocations).fold(permittedLocations(target)) { _ & _ }
        if (intersectPermittedLocs.nonEmpty) {
          intersectPermittedLocs
        } else {
          throw new NoLocationAvailable(target +: arguments.toSeq)
        }
      }
    }
    Logger.finest(s"candidateDestinations=$candidateDestinations")
    val destination = pickLocation(candidateDestinations)
    sendCall(callHandler, target, arguments, destination)
  }

  /* Since we don't have token IDs in PorcE: */
  private val callCorrelationCounter = new AtomicLong(followerExecutionNum.toLong << 32)

  def sendCall(callContext: CPSCallResponseHandler, callTarget: AnyRef, callArguments: Array[AnyRef], destination: PeerLocation): Unit = {
    Logger.fine(s"sendCall $callContext, $callTarget, $callArguments, $destination")

    val counterProxyId = makeProxyWithinCounter(callContext.c)

    val terminatorProxyId = makeProxyWithinTerminator(callContext.t, (terminatorProxyId) => sendKilled(destination, terminatorProxyId)())

    val callMemento = new CallMemento(callContext, counterProxyId = counterProxyId, terminatorProxyId = terminatorProxyId, target = callTarget, arguments = callArguments)

    val callCorrelationId = callCorrelationCounter.getAndIncrement()
    Tracer.traceCallSend(callCorrelationId, execution.runtime.here, destination)
    destination.sendInContext(execution)(MigrateCallCmd(executionId, callCorrelationId, callMemento))
  }

  def receiveCall(origin: PeerLocation, callCorrelationId: Long, callMemento: CallMemento): Unit = {
    // Token: Pass the token on the remote counter to the proxy counter.
    val proxyCounter = makeProxyCounterFor(callMemento.counterProxyId, origin)
    // TODO: When we reenable the proxy chain flattening optimization we will need to handle this more carefully because "proxyCounter" here could be a local counter or something else not quite what I am assuming here.    
    proxyCounter.takeParentToken()

    val proxyTerminator = makeProxyTerminatorFor(callMemento.terminatorProxyId, origin)

    val callInvoker = new Schedulable {
      def run(): Unit = {
        execution.invokeCallTarget(callMemento.callSiteId, callMemento.publicationContinuation, proxyCounter, proxyTerminator, callMemento.target, callMemento.arguments)
      }
    }
    /* invokeCallTarget will add an appropriate child to the proxyTerminator */

    Tracer.traceCallReceive(callCorrelationId, origin, execution.runtime.here)

    Logger.fine(s"scheduling $callInvoker")
    // Token: Pass the initial token of the proxy counter to the call.
    execution.runtime.schedule(callInvoker)
  }

  def sendPublish(destination: PeerLocation, proxyId: RemoteRef#RemoteRefId)(publicationContinuation: PorcEClosure, publishedValue: AnyRef): Unit = {
    Logger.fine(s"sendPublish: publish by proxyId $proxyId")
    Tracer.tracePublishSend(proxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(PublishGroupCmd(execution.executionId, proxyId, PublishMemento(publicationContinuation, publishedValue)))
  }

  def publishInGroup(origin: PeerLocation, groupMemberProxyId: RemoteRef#RemoteRefId, publication: PublishMemento): Unit = {
    Logger.entering(getClass.getName, "publishInGroup", Seq(groupMemberProxyId.toString, publication))
    val g = proxiedTerminatorMembers.get(groupMemberProxyId)
    if (g != null) {
      Tracer.tracePublishReceive(groupMemberProxyId, origin, execution.runtime.here)
      execution.runtime.schedule(CallClosureSchedulable(publication.publicationContinuation, publication.publishedValue))
    } else {
      throw new AssertionError(f"Publish by unknown group member proxy $groupMemberProxyId%#x, value=${publication.publishedValue}")
    }
  }
}

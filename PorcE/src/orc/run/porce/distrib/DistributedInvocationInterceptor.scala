//
// InvocationInterceptor.scala -- Scala traits InvocationInterceptor, NoInvocationInterception, and DistributedInvocationInterceptor
// Project PorcE
//
// Created by jthywiss on Dec 21, 2015.
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

/** Intercept external calls from a DOrcExecution, and possibly migrate them to another Location.
  *
  * @author jthywiss
  * @author amp
  */
trait DistributedInvocationInterceptor extends InvocationInterceptor {
  self: DOrcExecution =>

  override def shouldInterceptInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    // WARNING: Contains return!!!

    // TODO: PERFORMANCE: This would probably gain a lot by specializing on the number of arguments. That will probably require a simpler structure for the loops.
    if (target.isInstanceOf[RemoteRef] || arguments.exists(_.isInstanceOf[RemoteRef])) {
      return true
    } else {
      val here = runtime.here
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
    orc.run.distrib.Logger.finest(s"siteCall($target,$arguments): intersection of current locations=$intersectLocs")
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
    orc.run.distrib.Logger.finest(s"candidateDestinations=$candidateDestinations")
    val destination = pickLocation(candidateDestinations)
    sendCall(callHandler, target, arguments, destination)
  }

  /* Since we don't have token IDs in PorcE: */
  private val callCorrelationCounter = new AtomicLong(followerExecutionNum.toLong << 32)

  def sendCall(callContext: CPSCallResponseHandler, callTarget: AnyRef, callArguments: Array[AnyRef], destination: PeerLocation): Unit = {
    Logger.fine(s"sendCall $callContext, $callTarget, $callArguments, $destination")
    val publicationContinuation = callContext.p
    val enclosingCounter = callContext.c
    val enclosingTerminator = callContext.t

    val counterProxyId = enclosingCounter match {
      case cp: RemoteCounterProxy => cp.remoteProxyId
      case _ => freshRemoteRefId()
    }
    val rmtCounterMbrProxy = new RemoteCounterMembersProxy(counterProxyId, enclosingCounter)
    proxiedCounterMembers.put(counterProxyId, rmtCounterMbrProxy)

    /* enclosingCounter value is correct as is, since we're swapping this call with its proxy. */

    val terminatorProxyId = enclosingTerminator match {
      case tp: RemoteTerminatorProxy => tp.remoteProxyId
      case _ => freshRemoteRefId()
    }

    val rmtTerminatorMbrProxy = new RemoteTerminatorMembersProxy(terminatorProxyId, enclosingCounter, enclosingTerminator, () => sendKill(destination, terminatorProxyId)())
    proxiedTerminatorMembers.put(terminatorProxyId, rmtTerminatorMbrProxy)

    /* Swap call with rmtTerminatorMbrProxy in enclosingTerminator */
    enclosingTerminator.addChild(rmtTerminatorMbrProxy)
    enclosingTerminator.removeChild(callContext)

    val callMemento = new CallMemento(callContext, counterProxyId = counterProxyId, terminatorProxyId = terminatorProxyId, target = callTarget, arguments = callArguments)

    val callCorrelationId = callCorrelationCounter.getAndIncrement()
    Tracer.traceCallSend(callCorrelationId, self.runtime.here, destination)
    destination.sendInContext(self)(MigrateCallCmd(executionId, callCorrelationId, callMemento))
  }

  def receiveCall(origin: PeerLocation, callCorrelationId: Long, callMemento: CallMemento): Unit = {
    val lookedUpProxyCounterMember = proxiedCounterMembers.get(callMemento.counterProxyId)
    val proxyCounter = lookedUpProxyCounterMember match {
      case null => { /* Not a counter we've seen before */
        val rcp = new RemoteCounterProxy(callMemento.counterProxyId, () => sendHalt(origin, callMemento.counterProxyId)(), () => sendDiscorporate(origin, callMemento.counterProxyId)(), () => sendResurrect(origin, callMemento.counterProxyId)())
        proxiedCounters.put(callMemento.counterProxyId, rcp)
        rcp
      }
      case rcmp => rcmp.enclosingCounter
    }

    val lookedUpProxyTerminatorMember = proxiedTerminatorMembers.get(callMemento.terminatorProxyId)
    val proxyTerminator = lookedUpProxyTerminatorMember match {
      case null => { /* Not a terminator we've seen before */
        val rtp = new RemoteTerminatorProxy(callMemento.terminatorProxyId)
        proxiedTerminators.put(callMemento.terminatorProxyId, rtp)
        rtp
      }
      case rtmp => rtmp.enclosingTerminator
    }

    val callInvoker = new Schedulable { def run(): Unit = { self.invokeCallTarget(callMemento.callSiteId, ???, proxyCounter, proxyTerminator, callMemento.target, callMemento.arguments) } }
    proxyCounter.newToken()
    //invokeCallTarget adds an appropriate child the proxyTerminator

    if (lookedUpProxyCounterMember != null) {
      /* There is a RemoteCounterMembersProxy already for this proxyId,
       * so discard the superfluous one created at the sender. */
      Tracer.traceHaltGroupMemberSend(callMemento.counterProxyId, self.runtime.here, origin)
      origin.sendInContext(self)(HaltGroupMemberProxyCmd(executionId, callMemento.counterProxyId))
    }

    if (lookedUpProxyTerminatorMember != null) {
      /* There is a RemoteTerminatorMembersProxy already for this proxyId,
       * so discard the superfluous one created at the sender. */
      //Tracer.trace???Send(callMemento.terminatorProxyId, self.runtime.here, origin)
      //origin.sendInContext(self)(???(executionId, callMemento.terminatorProxyId))
      ???
    }

    Tracer.traceCallReceive(callCorrelationId, origin, self.runtime.here)

    Logger.fine(s"scheduling $callInvoker")
    runtime.schedule(callInvoker)
  }

  def sendPublish(destination: PeerLocation, proxyId: RemoteRef#RemoteRefId)(publishedValue: AnyRef): Unit = {
    Logger.fine(s"sendPublish: publish by proxyId $proxyId")
    Tracer.tracePublishSend(proxyId, self.runtime.here, destination)
    destination.sendInContext(self)(PublishGroupCmd(executionId, proxyId, PublishMemento(publishedValue)))
  }

  def publishInGroup(origin: PeerLocation, groupMemberProxyId: RemoteRef#RemoteRefId, publication: PublishMemento): Unit = {
    Logger.entering(getClass.getName, "publishInGroup", Seq(groupMemberProxyId.toString, publication))
    val g = proxiedTerminatorMembers.get(groupMemberProxyId)
    if (g != null) {
      Tracer.tracePublishReceive(groupMemberProxyId, origin, self.runtime.here)
      runtime.schedule(CallClosureSchedulable(???, publication.publishedValue))
    } else {
      throw new AssertionError(f"Publish by unknown group member proxy $groupMemberProxyId%#x, value=${publication.publishedValue}")
    }
  }
}

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

import orc.run.porce.runtime.{ CPSCallResponseHandler, InvocationInterceptor }
import orc.run.porce.runtime.PorcEClosure
import orc.run.porce.runtime.Counter
import orc.run.porce.runtime.Terminator
import orc.Schedulable
import orc.run.porce.runtime.CallClosureSchedulable

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

  def sendCall(callContext: CPSCallResponseHandler, callTarget: AnyRef, callArguments: Array[AnyRef], destination: PeerLocation): Unit = {
    Logger.fine(s"sendCall $callContext, $callTarget, $callArguments, $destination")
    val publicationContinuation = callContext.p
    val enclosingCounter = callContext.c
    val enclosingTerminator = callContext.t

    val proxyId = enclosingTerminator match {
      case tp: RemoteGroupProxy => tp.remoteGroupProxy.remoteProxyId
      case _ => freshRemoteRefId()
    }
    val rmtProxy = new RemoteGroupMembersProxy(proxyId, publicationContinuation, enclosingCounter, enclosingTerminator, () => sendKill(destination, proxyId)())
    proxiedGroupMembers.put(proxyId, rmtProxy)

    enclosingTerminator.addChild(rmtProxy)
    enclosingTerminator.removeChild(callContext)

    /* Counter is correct as is, since we're swapping this call with its proxy. */

    val callMemento = CallMemento(callSiteId = callContext.callSiteId, callSitePosition = callContext.callSitePosition, target = callTarget, arguments = callArguments)

    Tracer.traceCallSend(proxyId, self.runtime.here, destination)
    destination.sendInContext(self)(MigrateCallCmd(executionId, proxyId, callMemento))
  }

  def receiveCall(origin: PeerLocation, proxyId: RemoteRef#RemoteRefId, callMemento: CallMemento): Unit = {
    val lookedUpProxyGroupMember = proxiedGroupMembers.get(proxyId)
    val (proxyPublicationContinuation: PorcEClosure, proxyCounter: Counter, proxyTerminator: Terminator) = lookedUpProxyGroupMember match {
      case null => { /* Not a token we've seen before */
        val rgp = new RemoteGroupProxy(proxyId, sendPublish(origin, proxyId)(_), () => sendHalt(origin, proxyId)(), () => sendDiscorporate(origin, proxyId)(), () => sendResurrect(origin, proxyId)())
        proxiedGroups.put(proxyId, rgp)
        (rgp.publicationContinuation, rgp.counter, rgp.terminator)
      }
      case gmp => (gmp.publicationContinuation, gmp.enclosingCounter, gmp.enclosingTerminator)
    }

    val callInvoker = new Schedulable { def run(): Unit = { self.invokeCallTarget(callMemento.callSiteId, proxyPublicationContinuation, proxyCounter, proxyTerminator, callMemento.target, callMemento.arguments) } }
    proxyCounter.newToken()
    //invokeCallTarget adds an appropriate child the proxyTerminator

    if (lookedUpProxyGroupMember != null) {
      /* We have a RemoteGroupMembersProxy already for this proxyId,
       * so discard the superfluous one created at the sender. */
      Tracer.traceHaltGroupMemberSend(proxyId, self.runtime.here, origin)
      origin.sendInContext(self)(HaltGroupMemberProxyCmd(executionId, proxyId))
    }

    Tracer.traceCallReceive(proxyId, origin, self.runtime.here)

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
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      Tracer.tracePublishReceive(groupMemberProxyId, origin, self.runtime.here)
      runtime.schedule(CallClosureSchedulable(g.publicationContinuation, publication.publishedValue))
    } else {
      throw new AssertionError(f"Publish by unknown group member proxy $groupMemberProxyId%#x, value=${publication.publishedValue}")
    }
  }
}

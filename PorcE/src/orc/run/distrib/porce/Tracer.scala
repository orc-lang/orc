//
// Tracer.scala -- Scala object Tracer
// Project PorcE
//
// Created by jthywiss on Feb 21, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

/** Tracer utility for orc.run.distrib.porce package.
  *
  * @see orc.util.Tracer
  * @author jthywiss
  */
object Tracer {

  final val CallSend = 11L
  orc.util.Tracer.registerEventTypeId(CallSend, "CallSend")

  final val CallReceive = 12L
  orc.util.Tracer.registerEventTypeId(CallReceive, "CallRecv")

  final val PublishSend = 13L
  orc.util.Tracer.registerEventTypeId(PublishSend, "PubSend ")

  final val PublishReceive = 14L
  orc.util.Tracer.registerEventTypeId(PublishReceive, "PubRecv ")

  final val KillGroupSend = 15L
  orc.util.Tracer.registerEventTypeId(KillGroupSend, "KillSend")

  final val HaltGroupMemberSend = 16L
  orc.util.Tracer.registerEventTypeId(HaltGroupMemberSend, "HaltSend")

  final val DiscorporateGroupMemberSend = 17L
  orc.util.Tracer.registerEventTypeId(DiscorporateGroupMemberSend, "DiscortS")

  final val ReadFutureSend = 18L
  orc.util.Tracer.registerEventTypeId(ReadFutureSend, "ReadFutS")

  final val DeliverFutureResultSend = 19L
  orc.util.Tracer.registerEventTypeId(DeliverFutureResultSend, "DelFutSd")

  final val ResolveFutureSend = 20L
  orc.util.Tracer.registerEventTypeId(ResolveFutureSend, "ResFutSd")

  final val OrcEventSend = 21L
  orc.util.Tracer.registerEventTypeId(OrcEventSend, "EventSnd")

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceTokenMigration = false

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceGroupMessaging = false

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceFutureRemoteReads = false

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceEventNotifications = false

  @inline
  def traceCallSend(tokenId: Long, origin: Location[_], destination: Location[_]): Unit = {
    if (traceTokenMigration) {
      orc.util.Tracer.trace(CallSend, tokenId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def traceCallReceive(tokenId: Long, origin: Location[_], destination: Location[_]): Unit = {
    if (traceTokenMigration) {
      orc.util.Tracer.trace(CallReceive, tokenId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def tracePublishSend(groupProxyId: RemoteRef#RemoteRefId, origin: Location[_], destination: Location[_]): Unit = {
    if (traceTokenMigration) {
      orc.util.Tracer.trace(PublishSend, groupProxyId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def tracePublishReceive(groupMemberProxyId: RemoteRef#RemoteRefId, origin: Location[_], destination: Location[_]): Unit = {
    if (traceTokenMigration) {
      orc.util.Tracer.trace(PublishReceive, groupMemberProxyId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def traceKillGroupSend(groupProxyId: RemoteRef#RemoteRefId, origin: Location[_], destination: Location[_]): Unit = {
    if (traceGroupMessaging) {
      orc.util.Tracer.trace(KillGroupSend, groupProxyId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def traceHaltGroupMemberSend(groupMemberProxyId: RemoteRef#RemoteRefId, origin: Location[_], destination: Location[_]): Unit = {
    if (traceGroupMessaging) {
      orc.util.Tracer.trace(HaltGroupMemberSend, groupMemberProxyId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def traceDiscorporateGroupMemberSend(groupMemberProxyId: RemoteRef#RemoteRefId, origin: Location[_], destination: Location[_]): Unit = {
    if (traceGroupMessaging) {
      orc.util.Tracer.trace(DiscorporateGroupMemberSend, groupMemberProxyId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def traceFutureReadSend(futureId: RemoteFutureRef#RemoteRefId, origin: Location[_], destination: Location[_]): Unit = {
    if (traceFutureRemoteReads) {
      orc.util.Tracer.trace(ReadFutureSend, futureId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def traceFutureResultSend(futureId: RemoteFutureRef#RemoteRefId, origin: Location[_], destination: Location[_]): Unit = {
    if (traceFutureRemoteReads) {
      orc.util.Tracer.trace(DeliverFutureResultSend, futureId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def traceFutureResolveSend(futureId: RemoteFutureRef#RemoteRefId, origin: Location[_], destination: Location[_]): Unit = {
    if (traceFutureRemoteReads) {
      orc.util.Tracer.trace(ResolveFutureSend, futureId, origin.runtimeId, destination.runtimeId)
    }
  }

  @inline
  def traceOrcEventSend(origin: Location[_], destination: Location[_]): Unit = {
    if (traceEventNotifications) {
      orc.util.Tracer.trace(OrcEventSend, 0L, origin.runtimeId, destination.runtimeId)
    }
  }

}

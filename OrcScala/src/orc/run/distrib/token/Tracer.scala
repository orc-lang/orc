//
// Tracer.scala -- Scala object Tracer
// Project OrcScala
//
// Created by jthywiss on Feb 21, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.token

import orc.OrcRuntime

/** Tracer utility for orc.run.distrib.token package.
  *
  * @see orc.util.Tracer
  * @author jthywiss
  */
object Tracer {

  final val TokenSend = 11L
  orc.util.Tracer.registerEventTypeId(TokenSend, "TokSend ")

  final val TokenReceive = 12L
  orc.util.Tracer.registerEventTypeId(TokenReceive, "TokRecv ")

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

  final val OrcEventSend = 20L
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
  def getRuntimeId(runtime: OrcRuntime) = runtime.asInstanceOf[DOrcRuntime].runtimeId.longValue

  @inline
  def traceTokenSend(token: orc.run.core.Token, destination: RuntimeRef[_]) {
    if (traceTokenMigration) {
      orc.util.Tracer.trace(TokenSend, token.debugId, getRuntimeId(token.runtime), destination.runtimeId.longValue)
    }
  }

  @inline
  def traceTokenReceive(token: orc.run.core.Token, origin: RuntimeRef[_]) {
    if (traceTokenMigration) {
      orc.util.Tracer.trace(TokenReceive, token.debugId, origin.runtimeId.longValue, getRuntimeId(token.runtime))
    }
  }

  @inline
  def tracePublishSend(token: orc.run.core.Token, destination: RuntimeRef[_]) {
    if (traceTokenMigration) {
      orc.util.Tracer.trace(PublishSend, token.debugId, getRuntimeId(token.runtime), destination.runtimeId.longValue)
    }
  }

  @inline
  def tracePublishReceive(token: orc.run.core.Token, origin: RuntimeRef[_]) {
    if (traceTokenMigration) {
      orc.util.Tracer.trace(PublishReceive, token.debugId, origin.runtimeId.longValue, getRuntimeId(token.runtime))
    }
  }

  @inline
  def traceKillGroupSend(groupProxyId: DOrcExecution#GroupProxyId, origin: RuntimeRef[_], destination: RuntimeRef[_]) {
    if (traceGroupMessaging) {
      orc.util.Tracer.trace(KillGroupSend, groupProxyId, origin.runtimeId.longValue, destination.runtimeId.longValue)
    }
  }

  @inline
  def traceHaltGroupMemberSend(groupMemberProxyId: DOrcExecution#GroupProxyId, origin: RuntimeRef[_], destination: RuntimeRef[_]) {
    if (traceGroupMessaging) {
      orc.util.Tracer.trace(HaltGroupMemberSend, groupMemberProxyId, origin.runtimeId.longValue, destination.runtimeId.longValue)
    }
  }

  @inline
  def traceDiscorporateGroupMemberSend(groupMemberProxyId: DOrcExecution#GroupProxyId, origin: RuntimeRef[_], destination: RuntimeRef[_]) {
    if (traceGroupMessaging) {
      orc.util.Tracer.trace(DiscorporateGroupMemberSend, groupMemberProxyId, origin.runtimeId.longValue, destination.runtimeId.longValue)
    }
  }

  @inline
  def traceFutureReadSend(futureId: RemoteFutureRef#RemoteRefId, origin: RuntimeRef[_], destination: RuntimeRef[_]) {
    if (traceFutureRemoteReads) {
      orc.util.Tracer.trace(ReadFutureSend, futureId, origin.runtimeId.longValue, destination.runtimeId.longValue)
    }
  }

  @inline
  def traceFutureResultSend(futureId: RemoteFutureRef#RemoteRefId, origin: RuntimeRef[_], destination: RuntimeRef[_]) {
    if (traceFutureRemoteReads) {
      orc.util.Tracer.trace(DeliverFutureResultSend, futureId, origin.runtimeId.longValue, destination.runtimeId.longValue)
    }
  }

  @inline
  def traceOrcEventSend(origin: RuntimeRef[_], destination: RuntimeRef[_]) {
    if (traceEventNotifications) {
      orc.util.Tracer.trace(OrcEventSend, 0L, origin.runtimeId.longValue, destination.runtimeId.longValue)
    }
  }

}

//
// ExecutionMashaler.scala -- Scala trait ExecutionMashaler, and classes ClosureReplacement, CounterReplacement, TerminatorReplacement, and FutureReplacement
// Project PorcE
//
// Created by jthywiss on Aug 21, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import orc.PublishedEvent
import orc.run.porce.runtime.{ Counter, Future, PorcEClosure, Terminator }

/** A DOrcExecution mix-in to marshal and unmarshal dOrc execution-internal
  * objects, such as tokens, groups, closures, counters, terminators, etc.
  *
  * Note that this is called during serialization, not during
  * ValueMarshaler.marshalValue/unmarshalValue calls.
  *
  * @author jthywiss
  */
trait ExecutionMashaler {
  execution: DOrcExecution =>

  val marshalExecutionObject: PartialFunction[(PeerLocation, AnyRef), AnyRef] = {
    case (destination, closure: PorcEClosure) => {
      val callTargetIndex = execution.callTargetToId(closure.body)
      /* Invoke ValueMarshaler.marshalValue on values in environments. */
      val marshaledEnvironent = closure.environment.map(_ match {
        /* Don't run the value marshaler on closures in the environment.
         * ObjectOutputStream will handle them, including cycles among
         * environments. */
        case cl: PorcEClosure => cl
        case cl: ClosureReplacement => cl
        case v => execution.marshalValue(destination)(v)
      })
      ClosureReplacement(callTargetIndex, marshaledEnvironent, closure.isRoutine)
    }
    case (destination, counter: Counter) => {
      // Token: Counters which are just in the context do not carry a token with them. So this does not effect tokens.
      val proxyId = execution.getDistributedCounterForCounter(counter).id
      CounterReplacement(proxyId)
    }
    case (destination, terminator: Terminator) => {
      val proxyId = execution.makeProxyWithinTerminator(terminator,
        (terminatorProxyId) => execution.sendKilled(destination, terminatorProxyId)())
      TerminatorReplacement(proxyId)
    }
    case (destination, future: Future) => {
      val id = execution.ensureFutureIsRemotelyAccessibleAndGetId(future)
      FutureReplacement(id, future.raceFreeResolution)
    }
    case (destination, PublishedEvent(v: AnyRef)) => {
      Logger.Marshal.finer("marshalExecutionObject on PublishedEvent")
      PublishedEvent(execution.marshalValue(destination)(v))
    }
  }

  val unmarshalExecutionObject: PartialFunction[(PeerLocation, AnyRef), AnyRef] = {
    case (origin, ClosureReplacement(callTargetIndex, environment, isRoutine)) => {
      val callTarget = execution.idToCallTarget(callTargetIndex)
      /* Invoke ValueMarshaler.unmarshalValue on values in environments. */
      val unmarshledEnvironment = environment.map(_ match {
        /* Don't run the value marshaler on closures in the environment.
         * ObjectInputStream will handle them, including cycles among
         * environments. */
        case cl: ClosureReplacement => cl
        case cl: PorcEClosure => cl
        case v => execution.unmarshalValue(origin)(v)
      })
      new PorcEClosure(unmarshledEnvironment, callTarget, isRoutine)
    }
    case (origin, CounterReplacement(proxyId)) => {
      // Token: Counters which are just in the context do not carry a token with them. So this does not effect tokens.
      execution.getDistributedCounterForId(proxyId).counter
    }
    case (origin, TerminatorReplacement(proxyId)) => {
      execution.makeProxyTerminatorFor(proxyId, origin)
    }
    case (origin, FutureReplacement(bindingId, raceFreeResolution)) => {
      execution.futureForId(bindingId, raceFreeResolution)
    }
    case (origin, PublishedEvent(v: AnyRef)) => {
      Logger.Marshal.finer("unmarshalExecutionObject on PublishedEvent")
      PublishedEvent(execution.unmarshalValue(origin)(v))
    }
  }
}

private final case class ClosureReplacement(callTargetIndex: Int, environment: Array[AnyRef], isRoutine: Boolean) extends Serializable

// Token: Does not carry a token.
private final case class CounterReplacement(proxyId: CounterProxyManager#DistributedCounterId) extends Serializable

private final case class TerminatorReplacement(proxyId: TerminatorProxyManager#TerminatorProxyId) extends Serializable

private final case class FutureReplacement(bindingId: RemoteFutureRef#RemoteRefId, raceFreeResolution: Boolean) extends Serializable

//
// ExecutionMashaler.scala -- Scala trait ExecutionMashaler, and classes ClosureReplacement, CounterReplacement, TerminatorReplacement, and FutureReplacement
// Project PorcE
//
// Created by jthywiss on Aug 21, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.run.porce.runtime.{ Counter, Future, PorcEClosure, Terminator }
import java.util.IdentityHashMap
import java.util.Collections

/** A DOrcExecution mix-in to marshal and unmarshal dOrc execution-internal
  * objects, such as tokens, groups, closures, counters, terminators, etc.
  *
  * @author jthywiss
  */
trait ExecutionMashaler {
  execution: DOrcExecution =>
  
  // FIXME: This leaks references to objects in the table. This needs to be week.
  val instanceTable = Collections.synchronizedMap(new IdentityHashMap[AnyRef, AnyRef]())

  val marshalExecutionObject: PartialFunction[(PeerLocation, AnyRef), AnyRef] = {
    case (destination, closure: PorcEClosure) => {
      val callTargetIndex = execution.callTargetToId(closure.body)
      val marshaledEnvironent = closure.environment.map(_ match {
        /* Don't run the value marshler on closures.  Closures can be copied
         * to any location.  Also, cycles in closure environments are
         * avoided here by not running the value marshler on closures. */
        case cl: PorcEClosure => cl
        case cl: ClosureReplacement => {
          throw new AssertionError(s"ClosureReplacement should not have been introduced yet: $cl")
        }
        case v => execution.marshalValue(destination)(v)
      })
      ClosureReplacement(callTargetIndex, marshaledEnvironent, closure.isRoutine)
    }
    case (destination, counter: Counter) => {
      val proxyId = execution.makeProxyWithinCounter(counter)
      CounterReplacement(proxyId)
    }
    case (destination, terminator: Terminator) => {
      val proxyId = execution.makeProxyWithinTerminator(terminator,
        (terminatorProxyId) => execution.sendKilled(destination, terminatorProxyId)())
      TerminatorReplacement(proxyId)
    }
    case (destination, future: Future) => {
      val id = execution.ensureFutureIsRemotelyAccessibleAndGetId(future)
      FutureReplacement(id)
    }
  }

  val unmarshalExecutionObject: PartialFunction[(PeerLocation, AnyRef), AnyRef] = {
    case (origin, old@ClosureReplacement(callTargetIndex, environment, isRoutine)) => {
      //Logger.fine(f"Unmarshaling $old ${System.identityHashCode(old)}%x")
      val callTarget = execution.idToCallTarget(callTargetIndex)
      val unmarshledEnvironment = Array.ofDim[AnyRef](environment.length)
      val replacement = new PorcEClosure(unmarshledEnvironment, callTarget, isRoutine)
      instanceTable.put(old, replacement)
      environment.zipWithIndex.foreach({
        /* Don't run the value marshaler on closures.  Closures can be copied
         * to any location.  Also, cycles in closure environments are
         * avoided here by not running the value marshaler on closures. */
        case (cl: PorcEClosure, i) => 
          unmarshledEnvironment(i) = cl
        case (v, i) => 
          //Logger.fine(f"Unmarshaling nested value $v ${System.identityHashCode(v)}%x")
          // This handles Closures and uses instanceTable to avoid recursion.
          unmarshledEnvironment(i) = instanceTable.computeIfAbsent(v, (k) => execution.unmarshalValue(origin)(v))
      })
      replacement
    }
    case (origin, CounterReplacement(proxyId)) => execution.makeProxyCounterFor(proxyId, origin)
    case (origin, TerminatorReplacement(proxyId)) => execution.makeProxyTerminatorFor(proxyId, origin)
    case (origin, FutureReplacement(bindingId)) => execution.futureForId(bindingId)
  }
}

private final case class ClosureReplacement(callTargetIndex: Int, environment: Array[AnyRef], isRoutine: Boolean) extends Serializable

private final case class CounterReplacement(proxyId: CounterProxyManager#CounterProxyId) extends Serializable

private final case class TerminatorReplacement(proxyId: TerminatorProxyManager#TerminatorProxyId) extends Serializable

private final case class FutureReplacement(bindingId: RemoteFutureRef#RemoteRefId) extends Serializable

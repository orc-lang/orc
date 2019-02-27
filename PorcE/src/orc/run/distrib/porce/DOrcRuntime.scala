//
// DOrcRuntime.scala -- Scala class DOrcRuntime and trait RuntimeRef
// Project PorcE
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import scala.collection.convert.Wrappers.JConcurrentMapWrapper

import orc.ast.porc.MethodCPS
import orc.run.distrib.{ AbstractLocation, ClusterLocations }
import orc.run.porce.runtime.PorcERuntime

/** Distributed Orc (dOrc) Runtime Engine.
  *
  * Rule of thumb: Orc Runtimes manage external interaction, with the
  * environment. Program state and engine-internal behavior is the bailiwick
  * of Orc Executions.
  *
  * @author jthywiss
  */
abstract class DOrcRuntime(val runtimeId: DOrcRuntime.RuntimeId, engineInstanceName: String)
  extends PorcERuntime(engineInstanceName, null)
  with ClusterLocations[PeerLocation] {

  type ProgramAST = MethodCPS

  def locationForRuntimeId(runtimeId: DOrcRuntime.RuntimeId): PeerLocation

  /** A thread ID 32-bit integer that can be combined with a thread local
    * counter to produce identifiers.
    *
    * WARNING: Uniqueness is attempted, but not guaranteed.  Indicative only,
    * for non-critical uses, such as debugging log/trace.
    *
    * We use the least significant 8 bits of the runtime number and the
    * least significant 24 bits of Java's thread ID.
    */
  override def runtimeDebugThreadId: Int = runtimeId.longValue.toInt << 24 | Thread.currentThread().getId.asInstanceOf[Int]

  class ExecutionMap[X]() extends JConcurrentMapWrapper(new java.util.concurrent.ConcurrentHashMap[DOrcExecution#ExecutionId, X]) {
    override def default(key: DOrcExecution#ExecutionId): X =
      throw new NoSuchElementException("Execution not found (program not loaded): " + key)
  }
  
}

object DOrcRuntime {
  /* For now, runtime IDs and Execution follower numbers are the same.  When
   * we host more than one execution in an engine, they will be different. */
  class RuntimeId(val longValue: Long) extends AnyVal
}

/** A reference to a Distributed Orc runtime participating in this cluster
  * of runtimes.  Destination can be sent commands.
  *
  * @author jthywiss
  */
trait RuntimeRef[-M <: OrcCmd] extends AbstractLocation {
  override def hashCode: Int = runtimeId.longValue.toInt
  override def equals(that: Any): Boolean = that match {
    case other: RuntimeRef[M] if this.getClass == that.getClass && this.runtimeId == other.runtimeId => true
    case _ => false
  }
  def send(message: M): Unit
  def sendInContext(execution: DOrcExecution)(message: M): Unit
  def runtimeId: DOrcRuntime.RuntimeId
}

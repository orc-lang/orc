//
// OrcRuntimeInterface.scala -- Interfaces for Orc runtime
// Project OrcScala
//
// Created by amp on July 12, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility

import orc.OrcEvent
import orc.values.Signal
import orc.error.OrcException
import orc.compile.parse.OrcSourceRange

/** The interface through which the environment response to site calls.
  *
  * Published values passed to publish and publishNonterminal may not be futures.
  */
class CallContext(underlying: orc.MaterializedCallContext) extends orc.MaterializedCallContext {

  def this(c: orc.VirtualCallContext) = {
    this(c.materialize())
  }

  // TODO: Consider making this a separate API that is not core to the Orc JVM API.
  /** Submit an event to the Orc runtime.
    */
  def notifyOrc(event: OrcEvent): Unit = underlying.notifyOrc(event)

  // TODO: Replace with onidle API.
  /** Specify that the call is quiescent and will remain so until it halts or is killed.
    */
  def setQuiescent(): Unit = underlying.setQuiescent()

  /** Publish a value from this call without halting the call.
    */
  def publishNonterminal(v: AnyRef): Unit = underlying.publishNonterminal(v)

  /** Publish a value from this call and halt the call.
    */
  override def publish(v: AnyRef): Unit = underlying.publish(v)

  def publish() { publish(Signal) }

  /** Halt this call without publishing a value.
    */
  def halt(): Unit = underlying.halt()

  def !!(e: OrcException): Unit = halt(e)

  /** Halt this call without publishing a value, providing an exception which caused the halt.
    */
  def halt(e: OrcException): Unit = underlying.halt(e)

  /** Notify the runtime that the call will never publish again, but will not halt.
    */
  def discorporate(): Unit = underlying.discorporate()

  /** Provide a source position from which this call was made.
    *
    * Some runtimes may always return None.
    */
  def callSitePosition: Option[OrcSourceRange] = underlying.callSitePosition

  /** Return true iff the caller has the right named.
    */
  def hasRight(rightName: String): Boolean = underlying.hasRight(rightName)

  /** Return true iff the call is still live (not killed).
    */
  def isLive: Boolean = underlying.isLive

  def execution = underlying.execution
  def runtime = underlying.runtime
}


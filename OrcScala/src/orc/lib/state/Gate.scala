//
// Gate.scala -- Orc site Gate
// Project OrcScala
//
// Created by amp on Oct, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import orc.run.distrib.PinnedPlacementPolicy
import orc.types.{ SignalType, SimpleFunctionType}
import orc.values.Signal
import java.util.concurrent.atomic.AtomicBoolean
import orc.values.sites.{
  NonBlockingSite,
  PartialSite0Simple,
  TalkativeSite,
  TotalSite0Simple,
  TypedSite
}

/** A site which publishes signal on the first call and halts in all other cases.
  *
  * Exactly one call will publish.
  */
final class Gate extends PartialSite0Simple
    with TypedSite
    with NonBlockingSite with TalkativeSite with PinnedPlacementPolicy {
  override val inlinable = true
  // TODO: Use VarHandle in Java 9.
  private val flag = new AtomicBoolean(false)

  def eval() = {
    if (flag.compareAndSet(false, true))
      Some(Signal)
    else
      None
  }

  override def toString = s"<Gate: ${if (flag.get()) "closed" else "open"}>"

  def orcType() = {
    SimpleFunctionType(SignalType)
  }
}

/** Site to create a new Gate.
  * @author amp
  */
object NewGate
    extends TotalSite0Simple
    with TypedSite
    with NonBlockingSite
    with TalkativeSite {
  override val inlinable = true
  def eval() = {
    new Gate()
  }

  def orcType() = {
    SimpleFunctionType(SimpleFunctionType(SignalType))
  }
}

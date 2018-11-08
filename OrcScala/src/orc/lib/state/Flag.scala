//
// Flag.scala -- Scala class Flag
// Project OrcScala
//
// Created by amp on Sep 8, 2015.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import orc.run.distrib.PinnedPlacementPolicy
import orc.types.{ JavaObjectType, SignalType, SimpleFunctionType }
import orc.values.Signal
import orc.values.sites.{ Effects, EffectFreeSite, FunctionalSite, LocalSingletonSite, NonBlockingSite, PartialSite1Simple, TalkativeSite, TotalSite0Simple, TotalSite1Simple, TypedSite }

final class Flag extends PinnedPlacementPolicy {
  @volatile
  var _value = false

  @inline
  def set(): Unit = {
    _value = true
  }

  @inline
  def get() = _value

  override def toString = s"<Flag: ${get()}>"
}

/** @author amp
  */
object NewFlag extends TotalSite0Simple with TypedSite with FunctionalSite with TalkativeSite with Serializable with LocalSingletonSite {
  override val inlinable = true
  def eval() = {
    new Flag()
  }

  def orcType() = {
    SimpleFunctionType(JavaObjectType(classOf[Flag]))
  }
}

/** @author amp
  */
object SetFlag extends TotalSite1Simple[Flag] with TypedSite with TalkativeSite with NonBlockingSite with Serializable with LocalSingletonSite {
  override val inlinable = true
  def eval(flag: Flag) = {
    flag.set()
    Signal
  }

  def orcType() = {
    SimpleFunctionType(JavaObjectType(classOf[Flag]), SignalType)
  }

  override def effects = Effects.BeforePub
}

object PublishIfNotSet extends PartialSite1Simple[Flag] with TypedSite with NonBlockingSite with EffectFreeSite with Serializable with LocalSingletonSite {
  override val inlinable = true
  def eval(flag: Flag) = {
    if (flag.get())
      None
    else
      Some(Signal)
  }

  def orcType() = {
    SimpleFunctionType(JavaObjectType(classOf[Flag]), SignalType)
  }
}

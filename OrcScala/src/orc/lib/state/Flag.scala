//
// Flag.scala -- Scala class Flag
// Project OrcScala
//
// Created by amp on Sep 8, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import java.util.concurrent.atomic.AtomicBoolean

import orc.error.runtime.ArgumentTypeMismatchException
import orc.types.{ JavaObjectType, SignalType, SimpleFunctionType }
import orc.values.Signal
import orc.values.sites.{ EffectFreeSite, Effects, FunctionalSite, NonBlockingSite, PartialSite1, TalkativeSite, TotalSite0, TotalSite1, TypedSite }

final class Flag {
  val _value = new AtomicBoolean(false)

  @inline
  def set(): Unit = {
    _value.set(true)
  }

  @inline
  def get() = _value.get()

  override def toString = s"<Flag: ${get()}>"
}

/**
  * @author amp
  */
object NewFlag extends TotalSite0 with TypedSite with FunctionalSite with TalkativeSite {
  def eval() = {
    new Flag()
  }

  def orcType() = {
    SimpleFunctionType(JavaObjectType(classOf[Flag]))
  }
}

/**
  * @author amp
  */
object SetFlag extends TotalSite1 with TypedSite with TalkativeSite with NonBlockingSite {
  def eval(arg: AnyRef) = {
    arg match {
      case flag: Flag => {
        flag.set()
      }
      case a => throw new ArgumentTypeMismatchException(0, "Flag", if (a != null) a.getClass().toString() else "null")
    }
    Signal
  }

  def orcType() = {
    SimpleFunctionType(JavaObjectType(classOf[Flag]), SignalType)
  }

  override def effects = Effects.BeforePub
}

object PublishIfNotSet extends PartialSite1 with TypedSite with NonBlockingSite with EffectFreeSite {
  def eval(arg: AnyRef) = {
    arg match {
      case flag: Flag => {
        if (flag.get())
          None
        else
          Some(Signal)
      }
      case a => throw new ArgumentTypeMismatchException(0, "Flag", if (a != null) a.getClass().toString() else "null")
    }
  }

  def orcType() = {
    SimpleFunctionType(JavaObjectType(classOf[Flag]), SignalType)
  }
}

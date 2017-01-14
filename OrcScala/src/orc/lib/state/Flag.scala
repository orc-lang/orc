//
// IterableToStream.scala -- Scala object IterableToStream
// Project OrcScala
//
// Created by dkitchin on Apr 11, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.state

import orc.values.sites.Site
import orc.values.sites.TypedSite
import orc.values.sites.TotalSite0
import orc.values.sites.TotalSite1
import orc.values.sites.PartialSite1
import java.lang.Iterable
import orc.compile.typecheck.Typeloader
import orc.lib.builtin.structured.ListType
import orc.types.JavaObjectType
import orc.error.runtime.ArgumentTypeMismatchException
import java.util.concurrent.atomic.AtomicBoolean
import orc.types.SimpleFunctionType
import orc.types.SignalType
import orc.values.Signal
import orc.values.sites.FunctionalSite
import orc.values.sites.TalkativeSite
import orc.values.sites.TalkativeSite
import orc.values.sites.NonBlockingSite
import orc.values.sites.Effects
import orc.values.sites.EffectFreeSite

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
        if(flag.get())
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

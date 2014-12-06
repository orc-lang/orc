//
// GraftGroup.scala -- Scala class PruningGroup
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.Schedulable

/** A GraftGroup is the group associated with expression g in val x = g # f.
  *
  * @author dkitchin, amp
  */
class GraftGroup(parent: Group) extends Subgroup(parent) {

  var state: GraftGroupState = ValueUnknown

  override def toString = super.toString + s"(state=${state})"
  
  private var _future = new Future(runtime)
  
  def future = {
    assert(_future ne null)
    _future
  }

  // Publishing is idempotent
  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    state match {
      case ValueUnknown => {
        state = ValuePublished
        // There should be no situations in which v is None. Just let it crash if it's not.
        _future.bind(v.get)
        _future = null
      }
      case _ => {}
    }
    
    t.halt()
  }

  def onHalt() = synchronized {
    state match {
      case ValueUnknown => {
        state = ValueSilent
        parent.remove(this)
        _future.stop()
        _future = null
      }
      case ValuePublished => {
        parent.remove(this)
      }
      case _ => {}
    }
  }
}

/** Possible states of a PruningGroup */
class GraftGroupState
case object ValueUnknown extends GraftGroupState
case object ValuePublished extends GraftGroupState
case object ValueSilent extends GraftGroupState

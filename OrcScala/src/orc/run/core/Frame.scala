//
// Frame.scala -- Scala trait Frame and subclasses
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.ast.oil.nameless.Expression
import scala.collection.immutable.Traversable
import scala.collection.TraversableLike
import scala.collection.mutable.Builder
import scala.collection.mutable.ListBuffer

/** @author dkitchin
  */
trait Frame extends Traversable[Frame] {
  def apply(t: Token, v: Option[AnyRef]): Unit
  // Be careful when using a Frame as a collection; it does not have a builder
}

case object EmptyFrame extends Frame {
  def apply(t: Token, v: Option[AnyRef]) = {
    throw new AssertionError("Cannot publish through an empty frame")
  }
  def foreach[U](f: Frame => U) = {}
}

trait CompositeFrame extends Frame {
  val previous: Frame
  def foreach[U](f: Frame => U) = { f(this); previous.foreach(f) }
  override def toString = stringPrefix + "(...)"
}

/** @author dkitchin
  */
case class BindingFrame(n: Int, val previous: Frame) extends CompositeFrame {
  def apply(t: Token, v: Option[AnyRef]) {
    t.pop()
    t.unbind(n)
    previous(t, v)
  }
}

/** @author dkitchin
  */
case class SequenceFrame(private[run] var _node: Expression, val previous: Frame) extends CompositeFrame {
  def node = _node
  def apply(t: Token, v: Option[AnyRef]) {
    t.pop()
    t.bind(BoundValue(v.get))
    t.move(node)
    t.stage()
  }
  override def toString = stringPrefix + "(" + node + ")"
}

/** @author dkitchin
  */
case class FunctionFrame(private[run] var _callpoint: Expression, env: List[Binding], val previous: Frame) extends CompositeFrame {
  def callpoint = _callpoint
  def apply(t: Token, v: Option[AnyRef]) {
    t.pop()
    t.jump(env)
    t.move(callpoint)
    previous(t, v)
  }
  override def toString = stringPrefix + "(" + callpoint + ")"
}

/** @author dkitchin
  */
case class FutureFrame(private[run]_k: (Option[AnyRef] => Unit), val previous: Frame) extends CompositeFrame {
  def k = _k
  def apply(t: Token, v: Option[AnyRef]) {
    t.pop()
    k(v)
  }
}

/** @author dkitchin
  */
case class GroupFrame(val previous: Frame) extends CompositeFrame {
  def apply(t: Token, v: Option[AnyRef]) {
    t.pop()
    t.getGroup().publish(t, v)
  }
}

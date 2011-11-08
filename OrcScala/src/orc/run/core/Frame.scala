//
// Frame.scala -- Scala trait Frame and subclasses
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.ast.oil.nameless.Expression

/**
  * @author dkitchin
  */
trait Frame {
  def apply(t: Token, v: AnyRef): Unit
}

/**
  * @author dkitchin
  */
case class BindingFrame(n: Int) extends Frame {
  def apply(t: Token, v: AnyRef) {
    t.unbind(n).publish(v)
  }
}

/**
  * @author dkitchin
  */
case class SequenceFrame(private[run] var _node: Expression) extends Frame {
  def node = _node
  def apply(t: Token, v: AnyRef) {
    t.bind(BoundValue(v))
    t.move(node)
    t.schedule()
  }
}

/**
  * @author dkitchin
  */
case class FunctionFrame(private[run] var _callpoint: Expression, env: List[Binding]) extends Frame {
  def callpoint = _callpoint
  def apply(t: Token, v: AnyRef) {
    t.jump(env)
    t.move(callpoint)
    t.publish(v)
  }
}

/**
  * @author dkitchin
  */
case class FutureFrame(private[run]_k: (Option[AnyRef] => Unit)) extends Frame {
  def k = _k
  def apply(t: Token, v: AnyRef) {
    val _v = v.asInstanceOf[Option[AnyRef]]
    k(_v)
  }
}

/**
  * @author dkitchin
  */
case object GroupFrame extends Frame {
  def apply(t: Token, v: AnyRef) {
    t.getGroup().publish(t, v)
  }
}

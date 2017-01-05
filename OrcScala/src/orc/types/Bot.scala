//
// Bot.scala -- Scala object Bot
// Project OrcScala
//
// Created by dkitchin on Nov 26, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

/** @author dkitchin
  */
case object Bot extends CallableType {

  override def toString = "Bot"
  override def join(that: Type): Type = that
  override def meet(that: Type): Type = this
  override def <(that: Type) = true

  def call(typeArgs: List[Type], argTypes: List[Type]) = Bot

}

//
// Builtin.scala -- Collection of objects implementing Orc fundamental sites
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.builtin

import orc.oil.nameless.Type
import orc.oil._
import orc.sites._


/**
 * @author dkitchin
 */
object Let extends TotalSite {
  override def name = "Let"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) = Literal(args)
}

// Logic

/**
 * @author dkitchin
 */
object If extends PartialSite {
  override def name = "If"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Literal({}))
      case _ => None
  }
}

/**
 * @author jthywiss
 */
object Not extends PartialSite {
  override def name = "Not"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Literal(false))
      case List(Literal(false)) => Some(Literal(true))
      case _ => None
  }
}

object Eq extends UnimplementedSite


// Constructors

object NilConstructor extends UnimplementedSite
object TupleConstructor extends UnimplementedSite
object RecordConstructor extends UnimplementedSite
object ConsConstructor extends UnimplementedSite

/**
 * @author dkitchin
 */
object SomeConstructor extends PartialSite {
  override def name = "If"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) =
    args match {
      case List(a) => Some(Literal(Some(a)))
      case _ => None
  }
}


object NoneConstructor extends UnimplementedSite


// Extractors

object NilExtractor extends UnimplementedSite
object ConsExtractor extends UnimplementedSite
object SomeExtractor extends UnimplementedSite
object NoneExtractor extends UnimplementedSite
object FindExtractor extends UnimplementedSite


// Site site

object SiteSite extends UnimplementedSite



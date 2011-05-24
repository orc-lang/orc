//
// Top.scala -- Scala object Top
// Project OrcScala
//
// $Id: Top.scala 2773 2011-04-20 01:12:36Z jthywissen $
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

/**
 * 
 *
 * @author dkitchin
 */
case object Top extends Type {
  override def toString = "Top"
  override def join(that: Type): Type = this
  override def meet(that: Type): Type = that
}

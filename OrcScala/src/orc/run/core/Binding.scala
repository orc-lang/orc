//
// Binding.scala -- Scala trait Binding and subclasses
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

/** @author dkitchin
  */
sealed abstract class Binding extends Serializable

/**  */
case class BoundValue(v: AnyRef) extends Binding

/**  */
case class BoundReadable(c: ReadableBlocker) extends Binding

/**  */
case object BoundStop extends Binding

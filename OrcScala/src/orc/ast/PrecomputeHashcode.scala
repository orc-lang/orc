//
// PrecomputeHashcode.scala-- Scala class/trait/object PrecomputeHash
// Project OrcScala
//
// Created by amp on Jul 3, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast

/** A mix-in trait which converts a case class heirarchy to precompute hashes instead
  * of recomputing them whenever they are needed.
  */
trait PrecomputeHashcode { self: Product =>
  // TODO: This would perform at least a little better by using a macro which
  //       can somehow inject this val into each case class using the autogenerated
  //       inline hashcode as the body. This would avoid using the generic hashcode
  //       implementation like we do here.
  override val hashCode: Int = scala.runtime.ScalaRunTime._hashCode(this)
}

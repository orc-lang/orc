//
// Wrapper.scala -- Scala trait Wrapper
// Project OrcScala
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.tojava

/** @author amp
  */
trait Wrapper {
  def underlying: AnyRef
}

object Wrapper {
  def unwrap(v: AnyRef) = v match {
    case w: Wrapper => w.underlying
    case _ => v
  }
}

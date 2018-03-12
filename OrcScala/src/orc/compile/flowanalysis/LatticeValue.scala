//
// LatticeValue.scala -- Scala trait LatticeValue
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.flowanalysis

trait LatticeValue[T <: LatticeValue[T]] {
  def combine(o: T): T
  def lessThan(o: T): Boolean
}

//
// OptimizerStatistics.scala -- Scala traits for optimizer statistics collection.
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile

import scala.collection.mutable

trait NamedOptimization {
  val name: String
}

trait OptimizerStatistics {
  private val _optimizationCounts = mutable.Map[String, Long]().withDefaultValue(0)

  def optimizationCounts: collection.Map[String, Long] = _optimizationCounts

  def countOptimization(s: String, n: Long = 1): Unit = {
    _optimizationCounts(s) += n
  }
  def countOptimization(o: NamedOptimization): Unit = {
    countOptimization(o.name, 1)
  }
  def countOptimization(o: NamedOptimization, n: Long): Unit = {
    countOptimization(o.name, n)
  }
}

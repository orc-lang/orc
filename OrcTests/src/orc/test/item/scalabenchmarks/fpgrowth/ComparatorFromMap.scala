//
// ComparatorFromMap.scala -- Scala benchmark component ComparatorFromMap
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.fpgrowth

import java.util.Comparator
import java.util.concurrent.atomic.LongAdder

object ComparatorFromMap {
  def apply[T <: Comparable[T]](m: java.util.Map[T, LongAdder]): Comparator[T] = {
    def extractSum(x: T) = Option(m.get(x)).map(_.sum()).getOrElse(Long.MinValue)
    //def extractHash(x: T) = Option(m.get(x)).map(_.##).getOrElse(Int.MinValue)
    
    (x, y) => {
      val b = extractSum(y) compare extractSum(x)
      if (b == 0) {
        x compareTo y
      } else b
    }
  }
}

//
// Threads.scala -- Scala benchmark Threads
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

object Threads extends BenchmarkApplication[Unit, Unit] {
  import scala.concurrent.ExecutionContext.Implicits.global

  val N = BenchmarkConfig.problemSizeLogScaledInt(200000, 2)

  // Lines: 6
  def threads(n: Int): Unit = {
    if (n != 0) {
      val t = Future {
        threads(n - 1)
      }
      threads(n - 1)
      Await.ready(t, Duration.Inf)
    }
  }

  def benchmark(arg: Unit): Unit = {
    threads(N)
  }

  def check(results: Unit): Boolean = {
    true
  }

  def setup(): Unit = {
  }

  val name: String = "Threads"

  val size: Int = math.pow(2, N).toInt
}

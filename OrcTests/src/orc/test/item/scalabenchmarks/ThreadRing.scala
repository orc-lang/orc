//
// ThreadRing.scala -- Scala benchmark ThreadRing
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import scala.concurrent.Channel

/*
Threadring
    * create 503 linked threads (named 1 to 503)
    * thread 503 should be linked to thread 1, forming an unbroken ring
    * pass a token to thread 1
    * pass the token from thread to thread N times
    * print the name of the last thread (1 to 503) to take the token

Description from
http://shootout.alioth.debian.org/u32q/benchmark.php?test=threadring&lang=all
*/

object ThreadRing extends BenchmarkApplication[Unit, Int] with ExpectedBenchmarkResult[Int] {
  import Util._

  // Lines: 7
  def threadRing(id: Int, m: Int, in: Channel[Int], next: Channel[Int]): Int = {
    val x = in.read
    if (m == x)
      id
    else {
      next.write(x + 1)
      threadRing(id, m, in, next)
    }
  }

  val N = 1019

  // Lines: 14 (5)
  def threadRingRunner(p: Int) = {
    val ring = (0 until N).map(_ => new Channel[Int]()).toArray
    ring(0).write(0)

    val result = synchronized {
      var result: Option[Int] = None
      val threads = (0 until N).map(i => thread {
        result = Some(threadRing(i + 1, p, ring(i), ring((i + 1) % N)))
        synchronized {
          notify()
        }
      })

      while (result.isEmpty) {
        wait()
      }

      threads.foreach(_.terminate())

      result.get
    }

    println(result)

    result
  }

  def benchmark(ctx: Unit) = {
    threadRingRunner(BenchmarkConfig.problemSizeScaledInt(2000)) +
    threadRingRunner(BenchmarkConfig.problemSizeScaledInt(20000))
  }

  val name: String = "ThreadRing"

  def setup(): Unit = ()

  val size: Int = BenchmarkConfig.problemSizeScaledInt(2000) + BenchmarkConfig.problemSizeScaledInt(20000)

  val expectedMap: Map[Int, Int] = Map(
      1 -> (2001 % N + 20001 % N),
      10 -> (20001 % N + 200001 % N),
      100 -> (200001 % N + 2000001 % N),
      )
}


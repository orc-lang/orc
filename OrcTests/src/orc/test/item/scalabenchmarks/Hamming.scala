//
// Hamming.scala -- Scala benchmark Hamming sequence
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import scala.collection.mutable.Buffer
import scala.concurrent.Channel

object Hamming extends BenchmarkApplication[Unit, List[Int]] with ExpectedBenchmarkResult[List[Int]] {
  import Util._

  type I = Int
  val I = Int

  type Putter[A] = (A) => Unit
  type Getter[A] = () => A

  val threads = Buffer[ControllableThread[Any]]()

  implicit class ChannelOps[A](val chan: Channel[A]) extends AnyVal {
    def put: Putter[A] = (x) => {
      //println(s"Write to channel: $x -> $chan")
      chan.write(x)
    }
    def get: Getter[A] = () => {
      //println(s"Reading from channel: $chan")
      chan.read
    }
  }

  def Fanout[A](c: Getter[A], cs: List[Putter[A]]) = thread {
    while (true) {
      val x = c()
      cs.foreach(_(x))
    }
  }

  def Fanin[A](cs: List[Getter[A]], c: Putter[A]) = {
    cs.map(c1 => thread {
      while (true) {
        c(c1())
      }
    })
  }

  def Trans[A, B](f: (A) => B, in: Getter[A], out: Putter[B]) = thread {
    while (true) {
      out(f(in()))
    }
  }

  def UniqueMerge(is: List[Getter[I]], o: Putter[I]) = {
    val n = is.size
    val tops = Array.fill[Option[I]](is.size)(None)

    def fillTops() = {
      (0 until n).map(i => {
        if (tops(i) == None)
          tops(i) = Some(is(i)())
      })
    }

    def getMin(): I = {
      fillTops()

      val (mi, mv, _) = tops.foldLeft((-1, None: Option[I], 0)) { (acc, v) =>
        val (mi, mv, i) = acc
        if (mv.isDefined && v.get >= mv.get)
          (mi, mv, i + 1)
        else
          (i, v, i + 1)
      }

      (0 until n).map(i => {
        if (tops(i) == mv)
          tops(i) = None
      })

      mv.get
    }

    thread {
      while (true) {
        o(getMin())
      }
    }
  }

  def getN[A](n: Int, chan: Channel[A]): List[A] = {
    (for (_ <- 0 until n) yield chan.read).toList
  }

  def makeChannels[A](n: Int) = (0 until n).map(i => new Channel[A]()).toList

  val N = BenchmarkConfig.problemSizeScaledInt(400)

  def benchmark(ctx: Unit): List[I] = {
    val chans @ List(out, out1, x2, x3, x5, x21, x31, x51) = makeChannels[I](8)

    threads += Fanout(out.get, List(x2.put, x3.put, x5.put, out1.put))
    threads += UniqueMerge(List(x21.get, x31.get, x51.get), out.put)
    threads += Trans[I, I](_ * 2, x2.get, x21.put)
    threads += Trans[I, I](_ * 3, x3.get, x31.put)
    threads += Trans[I, I](_ * 5, x5.get, x51.put)

    out.put(1)

    val r = getN(N, out1)

    threads.foreach(_.terminate())
    
    r
  }

  val name: String = "Hamming"

  def setup(): Unit = ()

  val size: Int = N

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x978c0600,
      10 -> 0xe2d103,
      100 -> 0x34c9642,
      )
}

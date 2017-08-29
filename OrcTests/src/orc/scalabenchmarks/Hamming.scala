// Hamming sequence
package orc.scalabenchmarks

import scala.collection.mutable.Buffer
import scala.concurrent.Channel

import orc.test.BenchmarkApplication

object Hamming extends BenchmarkApplication {
  import Util._

  type I = BigInt
  val I = BigInt

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

  def main(args: Array[String]): Unit = {
    val chans @ List(out, out1, x2, x3, x5, x21, x31, x51) = makeChannels[I](8)

    threads += Fanout(out.get, List(x2.put, x3.put, x5.put, out1.put))
    threads += UniqueMerge(List(x21.get, x31.get, x51.get), out.put)
    threads += Trans[I, I](_ * 2, x2.get, x21.put)
    threads += Trans[I, I](_ * 3, x3.get, x31.put)
    threads += Trans[I, I](_ * 5, x5.get, x51.put)

    out.put(1)

    println(getN(400, out1))

    threads.foreach(_.terminate())
  }
}

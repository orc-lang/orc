package orc.test.item.scalabenchmarks.fpgrowth

import scala.util.Random
import scala.collection.JavaConverters._

import java.util.ArrayList
import orc.test.item.scalabenchmarks.BenchmarkConfig
import orc.test.item.scalabenchmarks.ExpectedBenchmarkResult
import orc.test.item.scalabenchmarks.UnorderedHash
import scala.util.hashing.MurmurHash3

case class FrequentItemSet(items: Iterable[Long], support: Long) {
  override def hashCode(): Int = MurmurHash3.unorderedHash(items) + (support.## * 37)
  override def toString(): String = s"<${items.mkString(",")}:${support}>"
}

object FrequentItemSet {
  def apply(items: Iterable[Long], support: Long) = new FrequentItemSet(items, support)
  def apply(items: java.util.Collection[Long], support: Long) = new FrequentItemSet(items.asScala, support)
  def apply(items: Array[Long], support: Long) = new FrequentItemSet(items, support)
}

object FreqMineData extends ExpectedBenchmarkResult[ArrayList[ArrayList[Long]]] with UnorderedHash[ArrayList[ArrayList[Long]]] {
  import BenchmarkConfig._
  
  val (nTransactions, dataSize, uniqueItems) = {
    val data = generate()
    val dataSize = data.asScala.map(_.size).sum
    val uniqueItems = data.asScala.foldLeft(Set[Long]())((acc, t) => acc ++ t.asScala).size
    (data.size, dataSize, uniqueItems)
  }
  
  def generate(): ArrayList[ArrayList[Long]] = {
    generate(problemSizeScaledInt(1000))
  }
  
  def generate(nTrans: Int): ArrayList[ArrayList[Long]] = {
    val rnd = new Random(10) // Fixed seed PRNG
    
    def normalInt(mean: Double, stddev: Double): Long = {
      val v = rnd.nextGaussian() * stddev + mean
      (v.abs.ceil max 1).toLong
    }
    
    def logNormalInt(mean: Double, stddev: Double): Long = {
      val v = math.exp(rnd.nextGaussian() * stddev + mean)
      (v.abs.ceil max 1).toLong
    }
    
    val out = new ArrayList[ArrayList[Long]](nTrans)
    
    for (_ <- 0 until nTrans) {
      val n = logNormalInt(1.8, 0.8)
      val mean = normalInt(1, 2) + 1
      val is = for (_ <- 0 until n.toInt) yield {
        logNormalInt(mean, 2)
      }
      
      out.add(new ArrayList[Long](is.toSet.asJava))
    }
    
    out
  }
  
  def main(args: Array[String]): Unit = {
    for(t <- generate().asScala.take(1000)) {
      println(t.asScala.mkString(" "))
    }
    println(s"nTransactions = $nTransactions, dataSize = $dataSize, uniqueItems = $uniqueItems")
  }

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x5bc57a67,
      10 -> 0x93078f48,
      100 -> 0x38b58222,
      )
}
{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}

include "benchmark.inc"

import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
type Double = Top

val n = 10
val iters = 1
val nPartitions = 8

import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import class LongAdder = "java.util.concurrent.atomic.LongAdder"

import class Point = "orc.test.item.scalabenchmarks.kmeans.Point"
import class KMeans = "orc.test.item.scalabenchmarks.kmeans.KMeans"
import class KMeansData = "orc.test.item.scalabenchmarks.kmeans.KMeansData"

class PointAdder {
  val x = DoubleAdder()
  val y = DoubleAdder()
  val count = LongAdder()
  
  def add(p, n) = (x.add(p.x()), y.add(p.y()), count.add(n)) >> signal
  
  {-- Get the average of the added points. 
    If this is called while points are being added this may have transient errors since the counter, x, or y may include values not included in the others. -}
  def average() =
    val c = count.sum()
  	Point(x.sum() / c, y.sum() / c)
  
  def toString() = "<" + x + "," + y + ">"
}

def PointAdder() = new PointAdder

def nof(0, v) = v >> []
def nof(n, v) = v >> v : nof(n-1, v)

def flatten([]) = []
def flatten(l:ls) = append(l, flatten(ls))

def run(xs) =
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, arrayToList(centroids)))) >> stop
  def run'(i, centroids) = run'(i - 1, updateCentroids(xs, centroids))
  run'(iters, KMeans.takePointArray(n, xs))

def updateCentroids(xs, centroids) = 
  val pointAdders = listToArray(map({ PointAdder() }, arrayToList(centroids)))
  val partitionSize = Ceil((0.0 + xs.length?) / nPartitions)
  forBy(0, xs.length?, partitionSize) >index> (
    val _ = Println("Partition: " + index + " to " + (index + partitionSize) + " (" + xs.length? + ")")
    val p = KMeans.sumAndCountClusters(xs, centroids, index, index + partitionSize)
    val xs = p.productElement(0)
    val ys = p.productElement(1)
    val counts = p.productElement(2)
  
    upto(xs.length?) >i> (
      pointAdders(i)?.add(Point(xs(i)? / 1.0, ys(i)? / 1.0), counts(i)?)
    )
  ) >> stop ;
  listToArray(map({ _.average() }, arrayToList(pointAdders)))

val points = KMeansData.data()

benchmarkSized("KMeans-scala-inner", points.length?, { points }, run)

{-
BENCHMARK
-}
  
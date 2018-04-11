{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
type Double = Top

import class Point = "orc.test.item.scalabenchmarks.kmeans.Point"
import class KMeansData = "orc.test.item.scalabenchmarks.kmeans.KMeansData"
import class KMeans = "orc.test.item.scalabenchmarks.kmeans.KMeans"

import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import class LongAdder = "java.util.concurrent.atomic.LongAdder"

val n = KMeans.n()
val iters = KMeans.iters()

class PointAdder {
  val x = DoubleAdder()
  val y = DoubleAdder()
  val count = LongAdder()
  
  def add(p) = (x.add(p.x()), y.add(p.y()), count.add(1)) >> signal
  
  {-- Get the average of the added points. 
    If this is called while points are being added this may have transient errors since the counter, x, or y may include values not included in the others. -}
  def average() =
    val c = count.sum()
    Point(x.sum() / c, y.sum() / c)
  
  def toString() = "<" + x + "," + y + ">"
}

def PointAdder() = new PointAdder

def run(xs) =
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, arrayToList(centroids)))) >> centroids
  def run'(i, centroids) = run'(i - 1, updateCentroids(xs, centroids))
  run'(iters, KMeans.takePointArray(n, xs))

def updateCentroids(xs, centroids) = 
  val pointAdders = listToArray(map({ _ >> PointAdder() }, arrayToList(centroids)))
  forBy(0, xs.length?, 1) >i> (
    val p = xs(i)?
    pointAdders(KMeans.closestIndex(p, centroids))?.add(p)
  ) >> stop ;
  listToArray(map({ _.average() }, arrayToList(pointAdders)))  
  
def dist(x :: Point, y :: Point) = x.sub(y).modulus()

val points = KMeansData.data()

benchmarkSized("KMeans-scala-inner", points.length?, { points }, run, KMeansData.check)

{-
BENCHMARK
-}

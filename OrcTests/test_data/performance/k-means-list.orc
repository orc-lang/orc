{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}

include "benchmark.inc"

import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
type Double = Top

val n = 10
val iters = 1

import class Point = "orc.test.item.scalabenchmarks.Point"
import class KMeansData = "orc.test.item.scalabenchmarks.KMeansData"
import class KMeans = "orc.test.item.scalabenchmarks.KMeans"

import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import class LongAdder = "java.util.concurrent.atomic.LongAdder"

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
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, arrayToList(centroids)))) >> stop
  def run'(i, centroids) = run'(i - 1, updateCentroids(xs, centroids))
  run'(iters, listToArray(take(n, xs)))

def updateCentroids(xs, centroids) = 
  val pointAdders = listToArray(map({ PointAdder() }, arrayToList(centroids)))
  each(xs) >p> (
    pointAdders(closestIndex(p, centroids))?.add(p)
  ) >> stop ;
  listToArray(map({ _.average() }, arrayToList(pointAdders)))  

def minBy(f, l) =
  def minBy'([x]) = (f(x), x)
  def minBy'(x:xs) =
    val (min, v) = minBy'(xs)
    val m = f(x)
    if min :> m then
      (m, x)
    else
      (min, v)
  minBy'(l)(1)

def closestIndex(x :: Point, choices) =
  minBy({ dist(x, choices(_)?) }, range(0, choices.length?))
  
def dist(x :: Point, y :: Point) = x.sub(y).modulus()

val points = arrayToList(KMeansData.data())

benchmarkSized(length(points), {
  run(points)
})

{-
BENCHMARK
-}
  
{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}
import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

Sequentialize() >> (
include "benchmark.inc"


import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
type Double = Top

def smap(f, xs) = Sequentialize() >> ( 
  def h([], acc) = acc
  def h(x:xs, acc) =
    f(x) >y> h(xs, y : acc)
  h(xs, []) 
  )
  
def seach(xs) = Sequentialize() >> ( 
  def h([]) = stop
  def h(x:xs) = x | h(xs)
  h(xs) 
  )

val n = 10
val iters = 1
  
import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import class LongAdder = "java.util.concurrent.atomic.LongAdder"

import class Point = "orc.test.item.scalabenchmarks.Point"
import class KMeansData = "orc.test.item.scalabenchmarks.KMeansData"

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

def PointAdder() = Sequentialize() >> new PointAdder

def run(xs) = Sequentialize() >> (
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, centroids))) >> stop
  def run'(i, centroids) = 
    run'(i - 1, updateClusters(xs, centroids))
  run'(iters, take(n, xs))
  )

def updateClusters(xs, clusters) = Sequentialize() >> ( 
  val clusterAdders = ConcurrentHashMap()
  smap({ clusterAdders.put(_, PointAdder()) }, clusters) >>
  seach(xs) >x> (
  	clusterAdders.get(closest(x, clusters)).add(x) 
  ) >> stop ;
  smap({ clusterAdders.get(_).average() }, clusters)
  )

def minBy(f, l) = Sequentialize() >> (
  def minBy'([x]) = (f(x), x)
  def minBy'(x:xs) =
    val (min, v) = minBy'(xs)
    val m = min >> f(x)
    if min :> m then
      (m, x)
    else
      (min, v)
  minBy'(l)(1)
  )

def closest(x :: Point, choices) = Sequentialize() >>
  minBy({ dist(x, _) }, choices)
  
def dist(x :: Point, y :: Point) = Sequentialize() >> x.sub(y).modulus()

val points = arrayToList(KMeansData.data())

benchmark({ Sequentialize() >>
  run(points)
})

{-
BENCHMARK
-}
  
)
{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}

include "benchmark.inc"

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

def eachArray(a) = upto(a.length?) >i> a(i)?
def fillArray(n, f) = 
    Array(n) >a> 
    (upto(n) >i> a(i) := f() >> stop ; a)
def tabulateArray(n, f) = 
    Array(n) >a> 
    (upto(n) >i> a(i) := f(i) >> stop ; a)
def mapArray(f, a) =
    tabulateArray(a.length?, { f(a(_)?) })

def updateCentroids(xs, centroids) = 
  val pointAdders = fillArray(centroids.length?, { PointAdder() }) --listToArray(map({ _ >> PointAdder() }, arrayToList(centroids)))
  forBy(0, xs.length?, 1) >i> (
    val p = xs(i)?
    pointAdders(closestIndex(p, centroids))?.add(p)
  ) >> stop ;
  mapArray({ _.average() }, pointAdders)

{-
def minBy(f, l) = Sequentialize() >> (
  def minBy'([x]) = (f(x), x)
  def minBy'(x:xs) =
    val (min, v) = minBy'(xs)
    val m = f(x)
    if min :> m then
      (m, x)
    else
      (min, v)
  minBy'(l)(1)
  )
-}

def closestIndex(x :: Point, choices) = 
  def h(-1, minV, minI) = minI 
  def h(i, minV, minI) =
    val newV = dist(x, choices(i)?) 
    if newV <: minV then
      h(i - 1, newV, i)
    else
      h(i - 1, minV, minI) 
  h(choices.length? - 1, 10000000000, -1)
  
def dist(x :: Point, y :: Point) = x.sub(y).modulus()

val points = KMeansData.data()

benchmarkSized("KMeans", points.length?, { points }, run, KMeansData.check)

{-
BENCHMARK
-}
  
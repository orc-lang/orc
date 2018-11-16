{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

include "benchmark.inc"

import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
type Double = Top

def smap(f, xs) = Sequentialize() >> ( -- Inferable (recursion)
  def h([], acc) = acc
  def h(x:xs, acc) =
    f(x) >y> h(xs, y : acc)
  h(xs, []) 
  )
  
def seach(xs) = Sequentialize() >> (  -- Inferable (multiple publications)
  def h([]) = stop
  def h(x:xs) = x | h(xs)
  h(xs) 
  )

def sfor(low, high, f) = Sequentialize() >> ( -- Inferable (recursion)
  def h(i) if (i >= high) = signal
  def h(i) = f(i) >> h(i + 1)
  h(low)
  )

import class Point = "orc.test.item.scalabenchmarks.kmeans.Point"
import class KMeansData = "orc.test.item.scalabenchmarks.kmeans.KMeansData"
import class KMeans = "orc.test.item.scalabenchmarks.kmeans.KMeans"

import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import class LongAdder = "java.util.concurrent.atomic.LongAdder"

val n = KMeans.n()
val iters = KMeans.iters()

class PointAdder {
  val x
  val y
  val count
  
  def add(p) = Sequentialize() >> -- Hard due to lack of typing on p to give delay on p.x()
  	(x.add(p.x()), y.add(p.y()), count.add(1)) >> signal
  
  {-- Get the average of the added points. 
    If this is called while points are being added this may have transient errors since the counter, x, or y may include values not included in the others. -}
  def average() = Sequentialize() >> ( -- Inferable
    val c = count.sum()
  	Point(x.sum() / c, y.sum() / c)
  	)
  
  def toString() = "<" + x + "," + y + ">"
}

def PointAdder() =
  DoubleAdder() >x'>
  DoubleAdder() >y'>
  LongAdder() >count'>
  new PointAdder { val x = x' # val y = y' # val count = count' }

def run(xs) =
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, arrayToList(centroids)))) >> centroids
  def run'(i, centroids) = run'(i - 1, updateCentroids(xs, centroids))
  xs >> KMeans.takePointArray(n, xs) >cs> run' >>
  run'(iters, cs)

def eachArray(a) = upto(a.length?) >i> SinglePublication() >> a(i)?
def fillArray(n, f) = 
    Array(n) >a> 
    (upto(n) >i> SinglePublication() >> a(i) := f() >> stop ; a)
def tabulateArray(n, f) = 
    Array(n) >a> 
    (upto(n) >i> SinglePublication() >> a(i) := f(i) >> stop ; a)
def mapArray(f, a) =
    tabulateArray(a.length?, { SinglePublication() >> f(a(_)?) })

def updateCentroids(xs, centroids) = 
  val pointAdders = fillArray(centroids.length?, { PointAdder() }) --listToArray(map({ _ >> PointAdder() }, arrayToList(centroids))) 
  pointAdders >>
  forBy(0, xs.length?, 1) >i> Sequentialize() >> SinglePublication() >> ( -- Inferable
    val p = xs(i)?
    pointAdders(KMeans.closestIndex(p, centroids))?.add(p)
  ) >> stop ;
  mapArray({ _.average() }, pointAdders)

val points = KMeansData.data()

benchmarkSized("KMeans-scala-opt", points.length?, { points }, run, KMeansData.check)

{-
BENCHMARK
-}
  
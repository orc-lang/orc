{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}

include "benchmark.inc"

import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
type Double = Top

def readFile(fn) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  BufferedReader(FileReader(fn))  >in>
  (  def appendLinesFromIn(lines) =
      (in.readLine() ; null)  >nextLine>
      ( if nextLine = null
        then
          lines
        else
          appendLinesFromIn(nextLine:lines)
      )
    appendLinesFromIn([])
  )  >ss>
  in.close()  >>
  reverse(ss)

val n = 10
val iters = 1

def sq(x :: Double) = x * x 
  
class Point {
  val x :: Double
  val y :: Double
  
  def div(k) = Point(x / k, y / k)

  def add(p) = Point(x + p.x, y + p.y)
  def sub(p) = Point(x - p.x, y - p.y)

  def modulus() = sqrt(sq(x) + sq(y))
  
  def toString() = "<" + x + "," + y + ">"
}

def Point(x' :: Double, y' :: Double) = new Point { val x = x' # val y = y' }
--def Point(x' :: Double, y' :: Double) = x' >x''> y' >y''> new Point { val x = x'' # val y = y'' }

import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import class LongAdder = "java.util.concurrent.atomic.LongAdder"

class PointAdder {
  val x = DoubleAdder()
  val y = DoubleAdder()
  val count = LongAdder()
  
  def add(p) = (x.add(p.x), y.add(p.y), count.add(1)) >> signal
  
  {-- Get the average of the added points. 
    If this is called while points are being added this may have transient errors since the counter, x, or y may include values not included in the others. -}
  def average() =
    val c = count.sum()
  	Point(x.sum() / c, y.sum() / c)
  
  def toString() = "<" + x + "," + y + ">"
}

def PointAdder() = new PointAdder

def run(xs) =
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, centroids))) >> stop
  def run'(i, centroids) = 
    run'(i - 1, updateClusters(xs, centroids))
  run'(iters, take(n, xs))

def updateClusters(xs, clusters) = 
  val clusterAdders = ConcurrentHashMap()
  map({ clusterAdders.put(_, PointAdder()) }, clusters) >>
  each(xs) >x> (
  	clusterAdders.get(closest(x, clusters)).add(x) 
  ) >> stop ;
  map({ clusterAdders.get(_).average() }, clusters)
  

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

def closest(x :: Point, choices) =
  minBy({ dist(x, _) }, choices)
  
def dist(x :: Point, y :: Point) = x.sub(y).modulus()

def average(xs) = cfold({ _.add(_) }, xs).div(length(xs))

def readPoints(path) = 
  map(lambda([a, b]) = Point(a, b), ReadJSON(head(readFile(path))))

val data = readPoints("test_data/performance/points.json")
val points = append(data,append(data, data))

benchmarkSized("k-means-list-class", length(points), { points }, run)

{-
BENCHMARK
-}
  
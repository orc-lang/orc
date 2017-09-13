{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}

include "benchmark.inc"

import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
type Double = Top

def smap(f, xs) =
  def h([], acc) = acc
  def h(x:xs, acc) =
    f(x) >y> h(xs, y : acc)
  h(xs, []) 

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

import class Point = "orc.test.item.scalabenchmarks.Point"
import class KMeansData = "orc.test.item.scalabenchmarks.KMeansData"

def run(xs) =
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, centroids))) >> stop -- | clusters(xs, centroids)
  def run'(i, centroids) = 
    run'(i - 1, smap(average, clusters(xs, centroids)))
  run'(iters, take(n, xs))

def groupBy(f, l) =
  val map = ConcurrentHashMap()
  seqMap(lambda(x) = (
    val y = f(x)
    if map.contains(y) then
      map.get(y)
    else (
      val c = Channel()
      map.putIfAbsent(y, c) >c'> Iff(c' = null) >> c' ;
      c
    )
  ) >c> c.put(x), l) >>
  map

def clusters(xs, centroids) =
  smap({ _.getAll() }, iterableToList(groupBy({ closest(_, centroids) }, xs).values()))

def minBy(f, l) =
  def minBy'([x]) = (f(x), x)
  def minBy'(x:xs) =
    val (min, v) = minBy'(xs)
    val m = min >> f(x)
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

val points = KMeansData.data() -- readPoints("test_data/performance/points-small.json")

val _ = Println(length(points) + "\n" + unlines(map({ _.toString() }, take(5, points))))

benchmark({
  run(points)
})

{-
BENCHMARK
-}
  
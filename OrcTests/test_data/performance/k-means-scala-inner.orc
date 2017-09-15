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

import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import class LongAdder = "java.util.concurrent.atomic.LongAdder"

import class Point = "orc.test.item.scalabenchmarks.Point"
import class KMeans = "orc.test.item.scalabenchmarks.KMeans"
import class KMeansData = "orc.test.item.scalabenchmarks.KMeansData"

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

def consMultiple(_, []) = []
def consMultiple([], tails) = tails
def consMultiple(h:heads, t:tails) = (h:t) : consMultiple(heads, tails) 

def split(n, l) =
  def h([], acc) = acc
  def h(xs, acc) = acc >>
    (take(n, xs) ; xs) >heads>
    (drop(n, xs) ; []) >tail>
    consMultiple(heads, acc) >newAcc>
    h(tail, newAcc)
  h(l, nof(n, [])) 

def flatten([]) = []
def flatten(l:ls) = append(l, flatten(ls))

def appendMultiple([x]) = x
def appendMultiple(l:ls) =
   cfold(lambda(l, acc) = zipWith(append, l, acc), ls)

def run(xs) =
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, centroids))) >> stop
  def run'(i, centroids) = run'(i - 1, updateCentroids(xs, centroids))
  run'(iters, take(n, xs))

def updateCentroids(xs, centroids) = 
  val pointAdders = listToArray(map({ PointAdder() }, centroids))
  val splitXs = KMeans.split(8, xs)
  each(splitXs) >partition> (
    val p = KMeans.sumAndCountClusters(partition, centroids)
    val xs = p.productElement(0)
    val ys = p.productElement(1)
    val counts = p.productElement(2)
  
    upto(xs.length?) >i> (
      pointAdders(i)?.add(Point(xs(i)? / 1.0, ys(i)? / 1.0), counts(i)?)
    )
  ) >> stop ;
  map({ _.average() }, arrayToList(pointAdders))

def readPoints(path) = 
  map(lambda([a, b]) = Point(a, b), ReadJSON(head(readFile(path))))

val points = KMeansData.data() -- readPoints("test_data/performance/points.json")

--val _ = Println(length(points) + "\n" + unlines(map({ _.toString() }, take(5, points))))

benchmarkSized(length(points), {
  run(points)
})

{-
BENCHMARK
-}
  
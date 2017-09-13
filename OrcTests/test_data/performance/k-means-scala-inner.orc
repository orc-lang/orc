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

import class Point = "orc.test.item.scalabenchmarks.Point"
import class KMeans = "orc.test.item.scalabenchmarks.KMeans"
import class KMeansData = "orc.test.item.scalabenchmarks.KMeansData"

def clusters(cs, xs) = KMeans.clusters(cs, xs)

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
  -- clusters(data, centroids) map average
  val splitXs = KMeans.split(8, xs)
  val mappings = KMeans.appendMultiple(map({ clusters(_, centroids) }, splitXs))
  map(KMeans.average, mappings)

def readPoints(path) = 
  map(lambda([a, b]) = Point(a, b), ReadJSON(head(readFile(path))))

val points = KMeansData.data() -- readPoints("test_data/performance/points.json")

val _ = Println(length(points) + "\n" + unlines(map({ _.toString() }, take(5, points))))

benchmarkSized(length(points), {
  run(points)
})

{-
BENCHMARK
-}
  
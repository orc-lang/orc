{- K-Means benchmark.
 - 
 - Based on the microbenchmark in several languages at https://github.com/andreaferretti/kmeans
 -}

include "benchmark.inc"

type Double = Top

import class Point = "orc.test.item.scalabenchmarks.Point"
import class KMeans = "orc.test.item.scalabenchmarks.KMeans"
import class KMeansData = "orc.test.item.scalabenchmarks.KMeansData"

val n = 10
val iters = 1

def run(xs) =
  def run'(0, centroids) = Println(unlines(map({ _.toString() }, centroids))) >> stop
  def run'(i, centroids) = run'(i - 1, updateCentroids(xs, centroids))
  run'(iters, take(n, xs))

def updateCentroids(xs, centroids) = 
  val p = KMeans.sumAndCountClusters(xs, centroids)
  val xs = p.productElement(0)
  val ys = p.productElement(1)
  val counts = p.productElement(2)
  
  collect({ upto(xs.length?) >i> (
    val c = counts(i)?
    Point(xs(i)? / c, ys(i)? / c)
  )})


val points = KMeansData.data()

--val _ = Println(length(points) + "\n" + unlines(map({ _.toString() }, take(5, points))))

val _ = Println(Point(0.0, 1.0))

benchmark({
  run(points)
})

{-
BENCHMARK
-}
  
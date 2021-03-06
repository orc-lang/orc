{- Mandelbrot.orc -- Orc program Mandelbrot
 - 
 - Created by amp on Aug 15, 2012 10:25:54 AM
 -}

include "benchmark.inc"

import class Mandelbrot = "orc.test.item.scalabenchmarks.Mandelbrot"

type Complex = (Number, Number)

def plus((ar, ai), (br, bi)) = (ar + br, ai + bi)

def square((ar, ai)) = (ar*ar - ai*ai, ai*ar*2)

def distance((ar, ai)) = (ar*ar + ai*ai) ** 0.5

val threshold = 100
val steps = 10
val size = problemSizeSqrtScaledInt(100)
val resolution = 3.0 / size
val offset = size / 2.0

def point(c) =
	def inner(z, n) = 
		val next = plus(square(z), c) #
		val isIn = distance(z) <: threshold #
		(Ift(n :> steps) >> isIn) ; inner(next, n + 1)
	inner((0.0,0.0), 0)

def cell(i, j) = point(((i-offset)*resolution, (j-offset)*resolution))
def row(i) = Table(size, curry(cell)(i))

def tableToList(0, _) = []
def tableToList(n, t) = t(n-1) : tableToList(n-1, t)

def showRow(l) = afold({ _ + _ }, map({ if _ then "@" else "." }, l))

benchmarkSized("Mandelbrot", size * size, { signal }, lambda(_) =
  map({ tableToList(size, _) }, tableToList(size, Table(size, row))),
  Mandelbrot.check
)

{-
BENCHMARK
-}

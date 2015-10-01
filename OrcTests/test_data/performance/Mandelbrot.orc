{- Mandelbrot.orc -- Orc program Mandelbrot
 - 
 - $Id$
 - 
 - Created by amp on Aug 15, 2012 10:25:54 AM
 -}

type Complex = (Number, Number)

def plus(Complex, Complex) :: Complex
def plus((ar, ai), (br, bi)) = (ar + br, ai + bi)

def square(Complex) :: Complex
def square((ar, ai)) = (ar*ar - ai*ai, ai*ar*2)

def distance(Complex) :: Number
def distance((ar, ai)) = (ar**2 + ai**2) ** 0.5

val threshold = 100
val steps = 10
val size = 24
val resolution = 0.125 -- 3.0 / size
val offset = 12 -- size / 2.0

def point(c :: Complex) =
	def inner(z :: Complex, n :: Integer) :: Boolean = 
		val next = plus(square(z), c) #
		val isIn = distance(z) <: threshold #
		(Ift(n :> steps || ~isIn) >> isIn) ; inner(next, n + 1)
	inner((0,0), 0)

def cell(i :: Integer, j :: Integer) = point(((i-offset)*resolution, (j-offset)*resolution))
def row(i :: Integer) = Table(size, curry(cell)(i))

def tableToList[A](Integer, lambda(Integer) :: A) :: List[A]
def tableToList(0, _) = []
def tableToList(n, t) = t(n-1) : tableToList(n-1, t)

def showRow(l :: List[Boolean]) = afold(lambda(x::String, y::String) = x + y, 
			map(lambda(x::Boolean) = if x then "@" else ".", l))

tableToList(size, Table(size, row)) >ll>
map(compose(showRow, lambda(t :: lambda(Integer) :: Boolean) = tableToList(size, t)), ll) >ls>
Println(unlines(ls))

| 
Println("size = " + size + ", resolution = " + resolution + ", offset = " + offset)

{-
OUTPUT:
size = 24, resolution = 0.125, offset = 12
signal
........................
........................
........................
........................
........................
........................
........................
........................
......@.@@@.@@@.@.......
......@@@@@@@@@@@.......
......@@@@@@@@@@@.......
...@.@@@@@@@@@@@@@.@....
...@@@@@@@@@@@@@@@@@....
....@@@@@@@@@@@@@@@.....
......@@@@@@@@@@@.......
......@@@@@@@@@@@.......
......@@@@@@@@@@@.......
........@@@@@@@.........
.........@@@@@..........
.........@@@@@..........
.........@@@@@..........
........@@@@@@@.........
..........@@@...........
...........@............

signal
-}
{-
BENCHMARK
-}

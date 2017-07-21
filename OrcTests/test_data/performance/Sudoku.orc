{- Sudoku.orc -- Orc program Sudoku
 - Created by amp on Nov 10, 2012 2:08:01 PM
 -}

include "benchmark.inc"

type Board = List[ List[ Option[ Integer ] ] ]

def get(b, x, y) = index(index(b, y), x)

def replace(l:ls, v, 0) = v : ls
def replace(l:ls, v, n) = l : replace(ls, v, n-1)
  
def update(b, v, x, y) = replace(b, replace(index(b, y), v, x), y)

def rows(Board) :: Board
def rows(l) = l 

def cols(b) =
	if all({ ~empty(_) }, b) then 
		val n = cols(map(tail, b))
		map(head, b) : n
	else
		[]

def block_n(x, y, b::Board) = 
		concat(map( { take(3, drop(x*3, _)) },
					take(3, drop(y*3, b)) ))

def contents(l) =
	def flatenMaybeList([]) = []  
	def flatenMaybeList(Some(v) : xs) = v : flatenMaybeList(xs) 
	def flatenMaybeList(None() : xs) = flatenMaybeList(xs) 
	sortUnique(flatenMaybeList(l))

def constraints(x, y, b) = 
	val r = contents(index(rows(b), y))
	val c = contents(index(cols(b), x))
	val bl = contents(block_n(x/3, y/3, b))
	mergeUnique(r, mergeUnique(c, bl))

def remove(_, []) = []
def remove(e, x:xs) = if e = x then xs else x : remove(e, xs)

def possibleV(x, y, b) = 
	val c = get(b, x, y)
	c >None()> foldl({ _ >a> remove(_, a) }, range(1,10), constraints(x, y, b)) |
	c >Some(v)> [v]  
	
def solveCell(x, y, b) =
	each(possibleV(x, y, b)) >v> update(b, Some(v), x, y)

def ndfoldl[A,B](lambda (B, A) :: B, B, List[A]) :: B
def ndfoldl(f,z,[]) = z
def ndfoldl(f,z,x:xs) = f(z,x) >z'> ndfoldl(f,z',xs)

def solveRow(y, b) = ndfoldl({ _ >acc> solveCell(_, y, acc) }, b, range(0, 9))
def solveAlgo0(b) = ndfoldl({ _  >acc> solveRow(_, acc) }, b, range(0, 9))

def load(s) = 
  def parseCell(c) = 
  	  Ift(c = " ") >> None()
  	| (val v = Read[Integer](c)
  	  if v :> 0 && v <: 10 then Some(v) else stop)
  def parseLine(l) = map(parseCell, characters(l))
  map(parseLine, lines(s))

def printRow([]) = ""
def printRow(Some(v):cs) = " " + Write(v) + printRow(cs)
def printRow(None():cs) = " _" + printRow(cs) 

def printBoard([]) = ""
def printBoard(r : rs) = printRow(r) + "\n" + printBoard(rs)

-- This does not work
def printBoard2(b) = unlines(map(printRow, b))  	  	

val ex0 = "4 2  9 83\n  84 2   \n3968   7 \n  1   75 \n9   6   2\n 57   8  \n 2   6395\n   7 56  \n86 9  4 7"
val ex1 = "  7  5  2\n    31 8 \n 3    7  \n  8 2   3\n 73   82 \n5   6 9  \n  5    6 \n 1 59    \n8  4  2  "


val b = load(ex0)

benchmark({ 
	Println(printBoard(b)) >>
	Println("==================") >>
	solveAlgo0(b) >x> 
	Println(printBoard(x))
})

{-
BENCHMARK
-}

-- A simple deterministic sudoku solver

{-
Import utilities and setup wrappers
-}

import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"

def Set() = ConcurrentHashMap.newKeySet()

def seq(n) = 
  def h(i) if (i <: n) = i : h(i+1)
  def h(_) = []
  h(0) 

-- An object which selects the last remaining value from a set of possibilities 
class SelectLastValue {
  val possibilities
  
  val remaining = 
    val s = Set()
    each(possibilities) >x> s.add(x) >> stop ;
    s

  val valueCell = Cell()

  val value = valueCell.read() 
  
  def remove(x) = 
    remaining.remove(x) >r>
    (
      if r && remaining.size() = 1 then
        valueCell.write(remaining.iterator().next())
      else
        signal
    ) >> r
  
  def toString() = value.toString() 
}
def SelectLastValue(p) = new SelectLastValue { val possibilities = p }

-- A 2-d grid of values with an indexing operation and a method called to fill the cells
class Grid {
  -- Compute the value of (x, y).
  -- This may not block on get. However it may return an object that leniently blocks on get.
  def compute(Integer, Integer) :: Top
  
  val n :: Integer
  val m :: Integer
  
  val storage = 
    val a = Array(n * m)
    map(lambda ((x, y)) = a(x + y*n) := compute(x, y), 
        collect({ upto(n) >x> upto(m) >y> (x, y) })) >>
    a
   
  def get(x :: Integer, y :: Integer) = storage(x + y*n)?
  
  def toString() =
    "[\n" +
    foldl(lambda(acc, y) = 
      acc + foldl(lambda(acc, x) =
              acc + get(x, y).toString() + ", ", "", seq(n)) + "\n",
      "", seq(n)) + "]"
}

{- 
The solver represents unsolved cells as unbound futures on objects. Each
objects which represents a cell waits on the values of constraining cells
to reduce its own set of possibilities.
-}

-- The size of the sudoku puzzle, must be a square number (4, 9, 16, ...)
val X = -1

{-
val sqrtN = 2

val puzzle = [
1, X, 3, X,
X, X, 1, 2,
2, 3, X, 1,
X, 1, 2, X
]

val solution = [
1, 2, 3, 4,
3, 4, 1, 2,
2, 3, 4, 1,
4, 1, 2, 3
]
-- -}

val sqrtN = 3

-- {-
val puzzle = [
X, X, X, 2, 6, X, 7, X, 1,
6, 8, X, X, 7, X, X, 9, X,
1, 9, X, X, X, 4, 5, X, X,
8, 2, X, 1, X, X, X, 4, X,
X, X, 4, 6, X, 2, 9, X, X,
X, 5, X, X, X, 3, X, 2, 8,
X, X, 9, 3, X, X, X, 7, 4,
X, 4, X, X, 5, X, X, 3, 6,
7, X, 3, X, 1, 8, X, X, X
]

val solution = [
4, 3, 5, 2, 6, 9, 7, 8, 1,
6, 8, 2, 5, 7, 1, 4, 9, 3,
1, 9, 7, 8, 3, 4, 5, 6, 2,
8, 2, 6, 1, 9, 5, 3, 4, 7,
3, 7, 4, 6, 8, 2, 9, 1, 5,
9, 5, 1, 7, 4, 3, 6, 2, 8,
5, 1, 9, 3, 2, 6, 8, 7, 4,
2, 4, 8, 9, 5, 7, 1, 3, 6,
7, 6, 3, 4, 1, 8, 2, 5, 9
]
-- -}

{- 
val puzzle = [
X, 2, X, X, X, X, X, X, X,
X, X, X, 6, X, X, X, X, 3,
X, 7, 4, X, 8, X, X, X, X,
X, X, X, X, X, 3, X, X, 2,
X, 8, X, X, 4, X, X, 1, X,
6, X, X, 5, X, X, X, X, X,
X, X, X, X, 1, X, 7, 8, X,
5, X, X, X, X, 9, X, X, X,
X, X, X, X, X, X, X, 4, X
]

val solution = [
4, 3, 5, 2, 6, 9, 7, 8, 1,
6, 8, 2, 5, 7, 1, 4, 9, 3,
1, 9, 7, 8, 3, 4, 5, 6, 2,
8, 2, 6, 1, 9, 5, 3, 4, 7,
3, 7, 4, 6, 8, 2, 9, 1, 5,
9, 5, 1, 7, 4, 3, 6, 2, 8,
5, 1, 9, 3, 2, 6, 8, 7, 4,
2, 4, 8, 9, 5, 7, 1, 3, 6,
7, 6, 3, 4, 1, 8, 2, 5, 9
]
-- -}

val N = sqrtN * sqrtN

def allNumbers() = upto(N) >x> x
def allSqrtNumbers() = upto(sqrtN) >x> x

class SudokuCell {
  val value :: Integer
  
  val number = value + 1
  def toString() = number.toString()
}

{-
This can only solve sudoku puzzles where every step in immediately forced by the
existing state. If that is not the case it will stall. When such a stall takes place
the class will go quiescent, so onIdle could be used to detect this case and apply
guessing or heuristics.
-}

val solver = new Grid {
  val n = N
  val m = N
  
  val input = puzzle
  
  def getPuzzleCell(x, y) = 
    val v = index(input, x + y*N)
    if v :> 0 then
      Some(v - 1)
    else
      None()
  
  def makeUnknown(myX, myY) = new (SelectLastValue with SudokuCell) {
    val possibilities = collect(allNumbers)
    
    val _ = {|
      (
        allNumbers() >x> (x, myY) |
        allNumbers() >y> (myX, y) |
        allSqrtNumbers() >x> allSqrtNumbers() >y> ((myX / sqrtN) * sqrtN + x, (myY / sqrtN) * sqrtN + y)) 
          >(x,y)> 
      Iff(x = myX && y = myY) >>
      remove(get(x, y).value) >true> 
      -- Println("Removing " + get(x, y).value + " from " + (myX, myY) + " because of " + (x, y) + " leaving " + remaining) >>
      stop |
      value
    |}
    
    -- val _ = Println("Solved " + (myX, myY) + " with " + number)
  }
  def makeKnown(myX, myY, v) = new SudokuCell {
    val value = v 
  }
  
  def compute(myX, myY) = 
    val v = getPuzzleCell(myX, myY)
    v >Some(n)> makeKnown(myX, myY, n) |
    v >None()> makeUnknown(myX, myY)
}

Println(solver.toString()) >> stop

{-
OUTPUT:
[
4, 3, 5, 2, 6, 9, 7, 8, 1, 
6, 8, 2, 5, 7, 1, 4, 9, 3, 
1, 9, 7, 8, 3, 4, 5, 6, 2, 
8, 2, 6, 1, 9, 5, 3, 4, 7, 
3, 7, 4, 6, 8, 2, 9, 1, 5, 
9, 5, 1, 7, 4, 3, 6, 2, 8, 
5, 1, 9, 3, 2, 6, 8, 7, 4, 
2, 4, 8, 9, 5, 7, 1, 3, 6, 
7, 6, 3, 4, 1, 8, 2, 5, 9, 
]
signal
-}

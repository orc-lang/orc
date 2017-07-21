{- 8-queens.orc -- Orc program that solves the 8 queens chessboard puzzle
 - 
 - Created by dkitchin
 -}

include "benchmark.inc"

{- The position of a queen on the chessboard is a coordinate pair -}
type Queen = (Integer,Integer)

-- Publish if the queens cannot take one another.
def check(Queen,Queen) :: Signal
def check((a,b),(x,y)) = Ift(a /= x) >> Ift(b /= y) >> Ift(a - b /= x - y) >> Ift(a + b /= x + y)

-- Check a queen again each one in a list and then add it to the list if it is legal. Stop if not legal.
def addqueen(Queen, List[Queen]) :: List[Queen]
def addqueen(r, []) = [r]
def addqueen(r, q:qs) = check(r,q) >> q:(addqueen(r,qs))

-- Solve N-queens.
def queens(Integer) :: List[Queen]
def queens(N) =
  -- Extend the board given n times.
  def extend(List[Queen], Integer) :: List[Queen]
  def extend(x,0) = x
  def extend(x,n) = extend(x,n-1) >y> upto(N) >j> addqueen((n,j), y)
  extend([],N)

benchmark({
  collect(defer(queens, 8)) >x> signal
})

{-
BENCHMARK
-}

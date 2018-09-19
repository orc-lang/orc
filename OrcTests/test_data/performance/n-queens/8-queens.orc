{- 8-queens.orc -- Orc program that solves the 8 queens chessboard puzzle
 - 
 - Created by dkitchin
 -}

include "benchmark.inc"

import class NQueens = "orc.test.item.scalabenchmarks.NQueens"

{- The position of a queen on the chessboard is a coordinate pair -}
type Queen = (Integer,Integer)

-- Publish if the queens cannot take one another.
def check(Queen,Queen) :: Signal
def check((a,b),(x,y)) = Ift(a /= x) >> Ift(b /= y) >> Ift(Abs(x - a) /= Abs(y - b))

-- Check a queen again each one in a list and then add it to the list if it is legal. Stop if not legal.
def addqueen(Queen, List[Queen]) :: List[Queen]
def addqueen(r, []) = [r]
def addqueen(r, q:qs) = check(r,q) >> q:(addqueen(r,qs))

-- Solve N-queens.
def queens(Integer) :: List[Queen]
def queens(N) =
  -- Extend the board given n times.
  def extend(List[Queen], Integer) :: List[Queen]
  def extend(0) = []
  def extend(n) = extend(n-1) >y> upto(N) >j> addqueen((n,j), y)
  extend(N)

val N = Floor(8 + Log(problemSize)/Log(10))

def factorial(0) = 1
def factorial(1) = 1
def factorial(n) = n * factorial(n - 1)

benchmarkSized("N-Queens", factorial(N), { signal }, { _ >> collect({ queens(N) }) }, NQueens.check)

{-
BENCHMARK
-}

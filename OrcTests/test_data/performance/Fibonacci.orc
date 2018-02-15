include "benchmark.inc"

import class Fibonacci = "orc.test.item.scalabenchmarks.Fibonacci"

def fibNaive(Integer) :: Integer
def fibNaive(0) = 0
def fibNaive(1) = 1
def fibNaive(n) = fibNaive(n-1) + fibNaive(n-2)

def fibPair(Integer) :: Integer
def fibPair(n) = 
  def fib(Integer) :: (Integer, Integer)
  def fib(0) = (0, 0)
  def fib(1) = (1, 0)
  def fib(n) = 
    val (f1, f2) = fib(n-1) #
    (f1 + f2, f1)
  fib(n)(0)  

def fibPairTailrec(Integer) :: Integer
def fibPairTailrec(n) = 
  def fib(Integer, Integer, Integer) :: Integer
  def fib(i, curr, _) if (i = n) = curr
  def fib(i, curr, prev) = fib(i+1, curr + prev, curr)
  fib(0, 0, 1)

benchmarkSized("Fibonacci", 1, { signal }, { _ >> stop ;
fibPairTailrec(3000) >a>
fibPairTailrec(1000) >b>
fibPair(1000) >c>
fibNaive(19) >d>
[a, b, c, d]
}, Fibonacci.check)

{-
BENCHMARK
-}

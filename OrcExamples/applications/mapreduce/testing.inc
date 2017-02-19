{- testing.inc -- MapReduce components for testing
 -
 -}

import site Block = "orc.lib.util.Block"

def openPrintlnOutput() = new ReductionOutput {
  def write(k, v) = Println("OUT: " + k + " -> " + v)
  def close() = signal
}

def randomFailure[T, U](failureRate :: Number, f :: lambda(T) :: U) =
  lambda(x) =
    if URandom() <: failureRate then
      Println("Failing " + f + "(" + x + ")") >> 
      Block()
    else
      --Println("Succeeding " + f + "(" + x + ")") >> 
      f(x)
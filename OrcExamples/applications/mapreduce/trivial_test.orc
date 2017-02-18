{- trivial_test.orc -- Test the mapreduce library on a trivial work load. 
 -
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"

class MyMapper extends MapReduce {
  def map(v) =
    --Println("Map: " + v) >> stop |
    (v, 1)
}

class MyReducer extends MapReduce {
  def reduce(data) = 
    --Println("Reduce: " + data.key) >> stop |
    (data.key, data.reduce({ _ + _ }))
}

class MyReader extends MapReduce {
  val data = [
    "a",
    "a",
    "b",
    "c",
    "B",
    "c",
    "a",
    "a"
  ]
  
  def read() = each(data)
}

class MyWriter extends MapReduce {
  def openOutput() = new ReductionOutput {
    def write(k, v) = Println("OUT: " + k + " -> " + v)
    def close() = signal
  }
}

(new MyWriter with MyReader with MyMapper with MyReducer with MapReduceSimpleImplementation)() >> stop

{-
OUTPUT:PERMUTABLE:
OUT: b -> 1
OUT: B -> 1
OUT: c -> 2
OUT: a -> 4
-}
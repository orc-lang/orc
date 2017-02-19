{- trivial_test.orc -- Test the mapreduce library on a trivial work load. 
 -
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "engines.inc"
include "testing.inc"

def myMapper(v) =
  --Println("Map: " + v) >> stop |
  (v, 1)

def myReducer(data) = 
  --Println("Reduce: " + data.key) >> stop |
  (data.key, data.reduceAC({ _ + _ }))

def myReader() =  
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
  each(data)


val task = new MapReduce {
  val map = myMapper --randomFailure(0.5, myMapper)
  val reduce = myReducer
  val read = myReader
  val openOutput = openPrintlnOutput
} 

executeSimple(task, 2000) >> stop

{-
OUTPUT:PERMUTABLE:
OUT: b -> 1
OUT: B -> 1
OUT: c -> 2
OUT: a -> 4
-}
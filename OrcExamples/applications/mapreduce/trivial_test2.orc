{- trivial_test.orc -- Test the mapreduce library on a trivial work load. 
 -
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "engines.inc"
include "testing.inc"

val SingletonKey = "SINGLETON"

def totalsMapper(v) =
  v.codePointAt(0) >x> (SingletonKey, (x, 1))

val totalsReducer = 
  def reducePair(_, (l1, n1), (l2, n2)) = 
    --Println("Pairs " + ((l1, n1), (l2, n2))) >> 
    (l1 + l2, n1 + n2)
  binaryReducerAC(reducePair)

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
  val map = totalsMapper --randomFailure(0.5, myMapper)
  val reduce = totalsReducer
  val read = myReader
  val openOutput = openPrintlnOutput
} 

executeSimple(task, 10000) >> stop

{-
OUTPUT:
OUT: SINGLETON -> (750, 8)
-}
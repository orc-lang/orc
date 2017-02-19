{-
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "engines.inc"
include "timeIt.inc"
include "testing.inc"

val SingletonKey = "SINGLETON"

def totalsMapper(v) =
  (SingletonKey, (v, 1))


def totalsReducer() = 
  def reducePair(_, (l1, n1), (l2, n2)) = 
    -- Println("Pairs " + ((l1, n1), (l2, n2))) >> 
    (l1 + l2, n1 + n2)
  binaryReducerAC(reducePair)

def meanFromMapWriter(mr :: SimpleMapWriter) = 
  val (total_letters, total_words) = mr.finalOutput.get(SingletonKey)
  total_letters.doubleValue() / total_words

def reader() = 
  upto(10000) >> URandom()

def do() =
  val totals = new (MapReduce with SimpleMapWriter) {
    val read = reader
    val map = totalsMapper
    val reduce = totalsReducer()
  }
  val _ = executeDumb(totals)
  val mean = meanFromMapWriter(totals)
  
  Println("Average: " + mean)

timeIt(do)

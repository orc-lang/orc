{-
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "webtext.inc"

class MyMapper extends MapReduce {
  def map(v) =
    val words = v.toLowerCase().replaceAll("[^a-z\\s]", "").trim().split("\\s+")
    --Println("Map: " + v) >> stop |
    each(arrayToList(words)) >w> (w, 1)
}

class MyReducer extends MapReduce {
  def reduce(data) = 
    --Println("Reduce: " + data.key) >> stop |
    (data.key, data.reduce({ _ + _ }))
}

class MyWriter extends MapReduce {
  def openOutput() = new ReductionOutput {
    def write(k, v) = Println("OUT: " + k + " -> " + v)
    def close() = signal
  }
}

val engine = new (MyWriter with MyMapper with MyReducer with MapReduceSimpleImplementation with WebLineReader with LimitedReader) {
  val url = "http://www.singingwizard.org/stuff/pg24132.txt"  
  val itemLimit = 10000
}

engine() >> stop
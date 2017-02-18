{-
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "webtext.inc"

val SingletonKey = "SINGLETON"

class TotalsMapper extends MapReduce {
  def computeValue(Integer) :: Integer
  
  def map(v) =
    computeValue(v) >x> (SingletonKey, (x, 1))
}

class MapWriter extends MapReduce {
  val finalOutput = outputCell.read()
  
  val output = new Map
  val outputCell = Cell[Map]()
  
  def openOutput() = new ReductionOutput {
    def write(k, v) = output.put(k, v)
    def close() = outputCell.write(output)
  }
}

class PrintLnWriter extends MapReduce {
  def openOutput() = new ReductionOutput {
    def write(k, v) = Println("OUT: " + k + " -> " + v)
    def close() = signal
  }
}

class TotalsReducer extends BinaryReducer with MapWriter {
  def reducePair(_, (l1, n1), (l2, n2)) = 
    (l1 + l2, n1 + n2)
  
  val (total_letters, total_words) = finalOutput.get(SingletonKey)
  val mean = total_letters.doubleValue() / total_words
}

class WordLengthTotalsMapper extends TotalsMapper {
  def modify(v) = v
  
  def computeValue(v) =
    val words = v.toLowerCase().replaceAll("[^a-z\\s]", "").trim().split("\\s+")
    each(arrayToList(words)) >w> modify(w.length()) 
}

class WordLengthSqDiffMapper extends WordLengthTotalsMapper {
  val inputMean :: Integer
  
  def modify(v) = (v - inputMean) * (v - inputMean) 
}

val totals = new (MapWriter with WordLengthTotalsMapper with TotalsReducer with MapReduceSimpleImplementation with WebLineReader with LimitedReader) {
  val url = "http://www.singingwizard.org/stuff/pg24132.txt"  
  val itemLimit = 10000
}
val _ = totals()

val _ = Println("Average length: " + totals.mean)

val diffTotals =  new (MapWriter with WordLengthSqDiffMapper with TotalsReducer with MapReduceSimpleImplementation with WebLineReader with LimitedReader) {
  val url = "http://www.singingwizard.org/stuff/pg24132.txt"  
  val itemLimit = 10000
  
  val inputMean = totals.mean
}
val _ = diffTotals()

val _ = Println("Mean squared difference from mean length: " + diffTotals.mean)
val _ = Println("Std.dev. length: " + sqrt(diffTotals.mean))

stop

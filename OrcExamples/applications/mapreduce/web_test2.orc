{-
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "engines.inc"
include "webtext.inc"
include "testing.inc"

val SingletonKey = "SINGLETON"

def totalsMapper(computeValue :: lambda(Integer) :: Integer) = lambda(v) =
  computeValue(v) >x> (SingletonKey, (x, 1))


def totalsReducer() = 
  def reducePair(_, (l1, n1), (l2, n2)) = 
    (l1 + l2, n1 + n2)
  binaryReducer(reducePair)

def meanFromMapWriter(mr :: SimpleMapWriter) = 
  val (total_letters, total_words) = mr.finalOutput.get(SingletonKey)
  total_letters.doubleValue() / total_words

def wordLengthBasedTotalsMapper(modify) =
  def computeValue(v) =
    val words = v.toLowerCase().replaceAll("[^a-z\\s]", "").trim().split("\\s+")
    each(arrayToList(words)) >w> modify(w.length()) 
  totalsMapper(computeValue)

def wordLengthTotalsMapper() = wordLengthBasedTotalsMapper(lambda(x) = x)
def wordLengthSqDiffTotalsMapper(inputMean :: Number) = wordLengthBasedTotalsMapper(lambda(v) = (v - inputMean) * (v - inputMean) )

val reader = webLineReader("http://www.singingwizard.org/stuff/pg24132.txt")

val totals = new (MapReduce with SimpleMapWriter) {
  val read = reader
  val map = wordLengthTotalsMapper()
  val reduce = totalsReducer()
}
val _ = executeDumb(totals)
val mean = meanFromMapWriter(totals)

val _ = Println("Average length: " + mean)

val diffTotals =  new (MapReduce with SimpleMapWriter) {
  val read = reader
  val map = wordLengthSqDiffTotalsMapper(mean)
  val reduce = totalsReducer()
}
val _ = executeDumb(diffTotals)
val variance = meanFromMapWriter(diffTotals)

val _ = Println("Mean squared difference from mean length: " + variance)
val _ = Println("Std.dev. length: " + sqrt(variance))

stop

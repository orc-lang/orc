{-
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "engines.inc"
include "webtext.inc"
include "testing.inc"

def myMapper(v) =
  val words = v.toLowerCase().replaceAll("[^a-z\\s]", "").trim().split("\\s+")
  --Println("Map: " + v) >> stop |
  each(arrayToList(words)) >w> (w, 1)

def myReducer(data) = 
  --Println("Reduce: " + data.key) >> stop |
  (data.key, data.reduce({ _ + _ }))

val task = new MapReduce {
  val url = "http://www.singingwizard.org/stuff/pg24132.txt"  
  val itemLimit = 10000
  
  val map = randomFailure(0.01, myMapper)
  val reduce = myReducer
  val read = limitedReader(itemLimit, webLineReader(url))
  val openOutput = openPrintlnOutput
}

executeSimple(task, 3000) >> stop
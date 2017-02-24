{-
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "engines.inc"
include "timeIt.inc"
include "webtext.inc"
include "testing.inc"

val url = "http://www.singingwizard.org/stuff/pg24132.txt"
val reader = webLineReader(url)
val data = collect(reader) 

def myMapper(v) =
  val words = v.toLowerCase().replaceAll("[^a-z\\s]", "").trim().split("\\s+")
  --Println("Map: " + v) >> stop |
  each(arrayToList(words)) >w> (w, 1)

def myReducer(data) = 
  --Println("Reduce: " + data.key) >> stop |
  (data.key, data.reduce({ _ + _ }))

def uptoSeq(n :: Integer, f :: lambda(Integer) :: Signal) :: Signal =
  def iter(Integer) :: Signal
  def iter(i) if (i <: n) = f(i) >> iter(i+1)
  def iter(_) = signal
  iter(0)

uptoSeq(10, lambda(_) =
  val task = new (MapReduce with SimpleMapWriter) {  
    val map = myMapper
    val reduce = myReducer
    def read() = each(data) 
    -- val openOutput = openPrintlnOutput
  }
  val out = timeIt({
    executeDumb(task)
  })
  Println(task.finalOutput.get("the"))
)


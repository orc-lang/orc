{- tiny_local_oo_mapreduce.orc -- Simple mapreduce benchmark, mainly to test objects. 
 -
 - Created by amp on Feb 17, 2017
 -}

include "mapreduce.inc"
include "benchmark.inc"


def timeoutAndRestart[T](timeout :: Integer, f :: lambda() :: T) = 
  -- TODO: This should be an unordered channel.
  val chan = Channel()
  val isSuccess = {| 
    Rwait(timeout) >> false | 
    (f() >v> chan.put(v) >> stop ; true)
  |} 
  isSuccess >> chan.closeD() >> stop |
  (
    if isSuccess then
      repeat(chan.get)
    else
      timeoutAndRestart(timeout, f)
  )

def IteratorReductionData(k :: ReductionKey, data :: Bag) = new ReductionData {
  val iter = data.iterator()
  val lock = Semaphore(1)
  val key = k
  def item() = withLock(lock, { Ift(iter.hasNext()) >> iter.next() })
}


{-
A simple implementation of MapReduce with fault tolerence.

The provided timeout (ms) is used for both mappers and reducers.
-}
def executeSimple(mr :: MapReduce, timeout :: Integer) =
  val intermediateData = new Map
  -- Map phase
  {-
  Fault tolerance notes:
  This assumes magically that the monitor and restart happens somewhere different from the
  computation. We should figure out how to express this location separation in dOrc.
  -}
  mr.read() >d> timeoutAndRestart(timeout, { mr.map(d) }) >(k, v)> 
      intermediateData.getOrUpdate(k, { new Bag }).add(v) >> stop ; (
    -- Reduce phase
    val out = mr.openOutput()
    intermediateData.each() >(k, v)> (
      timeoutAndRestart(timeout, { mr.reduce(IteratorReductionData(k, v)) }) >(k', v')> out.write(k', v')
    ) >> stop ;
    out.close()
  ) >> signal


def myMapper(v) =
  (v, 1)

def myReducer(data) = 
  (data.key, data.reduceAC({ _ + _ }))

val data1 = [
    "a",
    "a",
    "b",
    "c",
    "B",
    "c",
    "a",
    "a",
    ""
  ]
val data2 = [
    "x",
    "B",
    "c",
    "a",
    "C",
    ""
  ]

val multiplier = problemSizeScaledInt(300)

def myReader() =  
  upto(multiplier) >> each(data1) >a> each(data2) >b> a + b 
  
def openPrintlnOutput() = new ReductionOutput {
  def write(k, v) = Println("OUT: " + k + " -> " + v)
  def close() = signal
}


benchmarkSized("TinyOOMapReduce", multiplier, { signal }, lambda(_) =
val task = new MapReduce {
  val map = myMapper
  val reduce = myReducer
  val read = myReader
  val openOutput = openPrintlnOutput
} 

executeSimple(task, problemSizeScaledInt(2000)) >> stop,
{ _ >> false }
)

{-
BENCHMARK
-}

{-
OUTPUT:PERMUTABLE:
OUT: ba -> 300
OUT: cC -> 600
OUT: x -> 300
OUT: cB -> 600
OUT: b -> 300
OUT: aa -> 1200
OUT: bB -> 300
OUT: Bc -> 300
OUT:  -> 300
OUT: Bx -> 300
OUT: ca -> 600
OUT: C -> 300
OUT: BB -> 300
OUT: B -> 600
OUT: aC -> 1200
OUT: bC -> 300
OUT: BC -> 300
OUT: ac -> 1200
OUT: Ba -> 300
OUT: aB -> 1200
OUT: c -> 900
OUT: cc -> 600
OUT: cx -> 600
OUT: bc -> 300
OUT: bx -> 300
OUT: ax -> 1200
OUT: a -> 1500
-}
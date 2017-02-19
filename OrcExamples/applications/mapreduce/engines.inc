{- MapReduce implementations based on the framework in mapreduce.inc
 -
 - Created by amp on Feb 18, 2017
 -}

{-
The implementations are each a function which takes a MapReduce instance
and executes based on the mapper, reducer, and I/O operations in it. The
result of the processing will either be stored as specified in stable 
storage or in the MapReduce instance.
-}

{-
A trivial implementation of MapReduce.

This implementation spawns all map calls concurrently and collects their results 
into a single Map. Once the maps all complete, this implementation runs all reducers
concurrently and concurrently writes their output. This implementation does not
provide any fault tolerence nor does it build a tree of reducers to perform
concurrent reduction.
-}
def executeDumb(mr :: MapReduce) =
  val intermediateData = new Map
  -- Map phase
  mr.read() >d> mr.map(d) >(k, v)> intermediateData.getOrUpdate(k, { new Bag }).add(v) >> stop ; (
    -- Reduce phase
    val out = mr.openOutput()
    intermediateData.each() >(k, v)> (
      val data = new ReductionData {
        val key = k
        def item() = v.takeD() 
      }
      mr.reduce(data) >(k', v')> out.write(k', v')
    ) >> stop ;
    out.close()
  ) >> signal

def timeoutAndRestart[T](timeout :: Integer, f :: lambda() :: T) = 
  -- TODO: This should be an unordered channel.
  val chan = Channel()
  val isSuccess = {| 
    Rwait(timeout) >> false | 
    (f() >v> chan.put(v) >> stop ; true)
  |} 
  isSuccess >> chan.closeD() >> stop |
  --Println("Called " + f + " success: " + isSuccess) |
  (
    if isSuccess then
      repeat(chan.get)
    else
      timeoutAndRestart(timeout, f)
  )

{-
def parallelReduce(mr :: MapReduce, out :: ReductionOutput, k :: ReductionKey, data :: Bag, itemsPerTask :: Integer) =
  def newDataBlock() =
    val count = Counter(itemsPerTask)
    val dataBlock = new Bag
    repeat({ count.dec() >> data.takeD() }) >v> dataBlock.add(v) >> stop ;
    Iff(dataBlock.isEmpty()) >> dataBlock
  
  def callReduce(data) = 
    timeoutAndRestart(timeout, { mr.reduce(data) })
  
  def makeAllDataBlocks() =
    val chan = Channel()
    val _ = repeat(newDataBlock) >b> chan.put() >> stop ; chan.closeD() 
    val first = chan.get()
    val second = first >> chan.get()
    -- If there are at least two blocks:
    second >> (first | second | repeat(chan.get()))
      callReduce(BlockReductionData(data)) >(k', v')> stop -- ???
      ;
    -- If there is one block:
    first >> callReduce(BlockReductionData(data)) >(k', v')> out.write(k', v')
-}

def IteratorReductionData(k :: ReductionKey, data :: Bag) = new ReductionData {
  val iter = data.iterator()
  -- TODO: Implement better concurrency if needed.
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
    -- TODO: Implement parallel reduction and fault tolerance on reduce calls.
    intermediateData.each() >(k, v)> (
      timeoutAndRestart(timeout, { mr.reduce(IteratorReductionData(k, v)) }) >(k', v')> out.write(k', v')
    ) >> stop ;
    out.close()
  ) >> signal

{- An Orc library implementing map reduce.
 -
 - Created by amp on Feb 17, 2017
 -}

include "datastructures.inc"

type InputItem = Top
type ReductionItem = Top
type ReductionKey = Top

{-
A set of reduction data items with the same key.

It provides the key and an interface to stream the data one item at a time.
-}
class ReductionData {
  {-
  The key associated with this data.
  -}
  val key :: ReductionKey
  
  {-
  Publish a data item from this data set.
  
  The items are returned in arbitrary order, but every item will be returned
  at some point and no item will be returned more than once. Multiple 
  concurrent calls to item are allowed.
  
  This halts silently if there is no more data.
  -}
  def item() :: ReductionItem

  {-
  Apply a binary reduction operator to the entire dataset.
  
  This will consume the data by calling next() until it halts silently.
  -}
  def reduce(f :: lambda(ReductionItem, ReductionItem) :: ReductionItem) :: (ReductionKey, ReductionItem) =
    def h(acc :: ReductionItem) :: ReductionItem = item() >x> h(f(acc, x)) ; acc #
    -- We must get the init value first because otherwise the item() call in h could take the 
    -- single value and make this item() call halt silently.
    item() >init> h(init)
}

{-
An output handle for reducer result data.
-}
class ReductionOutput {  
  {-
  Write out a key-value result pair.
  -}
  def write(ReductionKey, ReductionItem) :: Signal
  
  {-
  Close the output.
  
  After this is called, calls to write have undefined behavior.
  -}
  def close() :: Signal
}

class MapReduce {
  {-
  The user's map operation.
  
  This takes the input data item and then publishes any number of key value pairs as output.
  -}
  def map(InputItem) :: (ReductionKey, ReductionItem)
  
  {-
  The user's reduce operation. It must be associative and commutative.
  
  This takes a group of data items with the same key and then publishes any number of key value pairs as output.
  -}
  def reduce(ReductionData) :: (ReductionKey, ReductionItem)
  
  {-
  Read in the data set.
  
  This publishes all input data items.
  -}
  def read() :: InputItem
  
  {-
  Get an output handle to write out the results of processing.
  -}
  def openOutput() :: ReductionOutput
}

class BinaryReducer extends MapReduce {
  def reducePair(ReductionKey, ReductionItem, ReductionItem) :: (ReductionKey, ReductionItem)
  
  def reduce(data) = 
    (data.key, data.reduce({ reducePair(data.key, _, _) }))
}


{-
A simple implementation of MapReduce using the above framework.

This implementation spawns all map calls concurrently and collects their results 
into a Map. Once the maps all complete, this implementation runs all reducer
concurrently and concurrently writes their output.
-}
class MapReduceSimpleImplementation extends MapReduce {
  def apply() =
    val intermediateData = new Map
    -- Map phase
    read() >d> map(d) >(k, v)> intermediateData.getOrUpdate(k, { new Bag }).add(v) >> stop ; (
      -- Reduce phase
      val out = openOutput()
      intermediateData.each() >(k, v)> (
        val data = new ReductionData {
          val key = k
          def item() = v.takeD() 
        }
        reduce(data) >(k', v')> out.write(k', v')
      ) >> stop ;
      out.close()
    ) >> this
}




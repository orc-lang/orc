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

{-
Take a binary reduction operator and convert it into a reducer appropriate for MapReduce.reduce.
-}
def binaryReducer(lambda(ReductionKey, ReductionItem, ReductionItem) :: (ReductionKey, ReductionItem)) 
        :: lambda (ReductionData) :: (ReductionKey, ReductionItem)
def binaryReducer(reducePair) =
  lambda(data :: ReductionData) = (data.key, data.reduce({ reducePair(data.key, _, _) }))

{-
Writer which adds each output pair to the output map and sets outputCell to the map when
the mapreduce is complete.

outputCell may be stop if completion notification is not needed.
-}
def openMapWriter(output :: Map, outputCell :: Cell) = lambda() = new ReductionOutput {
  def write(k, v) = output.put(k, v)
  def close() = outputCell.write(output) ; signal
}

{-
A mix-in providing simple usage of openMapWriter.

This provides a finalOutput field which is bound to the output map when
the processing is complete.
-}
class SimpleMapWriter extends MapReduce {
  val finalOutput = outputCell.read()
  
  val output = new Map
  val outputCell = Cell[Map]()
  
  val openOutput = openMapWriter(output, outputCell)
}

 

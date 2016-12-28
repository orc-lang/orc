{------ Superposed computation
 - 
 - A set of base classes that represent process graphs and allow for superposition.
 - 
 - $Id$
 -
 - Created by amp in Jan, 2015
 -}

-- Given the list [x1, x2, ...] return [(0, x1), (1, x2), ...].
def zipWithIndex[A](xs :: List[A]) =
        def h(List[A], Integer) :: List[(Integer, A)]
        def h([], n) = []
        def h(x : xs, n) = (n, x) : h(xs, n+1)
        h(xs, 0)

-- Some utility interfaces to represent the ends of channels.
class Source {
  def get() :: Top
}

class Sink {
  def put(v) :: Signal
  def close() :: Signal
}

-- Merge messages from many channels
class def FaninChannel(cs :: List[Source]) :: FaninChannel extends Source {
  -- Build an internal channel
  val c = Channel()
  
  -- Setup an ongoing computation that transfers values on
  -- input channels to the output channels
  val _ = each(zipWithIndex(cs)) >(i, c')> repeat({ c'.get() >x> c.put((i, x)) }) 

  -- Allow users to get values from the output channel
  def get() = c.get()
}

--- An OO approach using deep mix-ins and the cake pattern for
--- extensible pattern matching.

-- The base for all distributed process graph algorithms.
class MessageBase {
  val proc :: ProcessorBase
  
  def toString() :: String
}
class ProcessorBase {
  def process(i :: Integer, m :: MessageBase) = signal
  
  val nInputs :: Integer
  val nOutputs :: Integer
  
  def sendMessage(Integer, MessageBase) :: Signal
}

-- A class that implements the concrete event processing loop.
-- Various versions of this could be implemented to distribute over
-- different channel types for instance.
class ChannelProcessorBase extends ProcessorBase {
  val inputs :: List[Source]
  val outputs :: List[Sink]
  
  val nInputs = length(inputs)
  val nOutputs = length(outputs)

  val allInput = FaninChannel(inputs)
  
  val _ = repeat({ allInput.get() >(i, m)> process(i, m) })

  def sendMessage(i, m) = index(outputs, i).put(m)
}

{------ Superposed computation
 - An example of superposing a Lamport clock and a distributed snapshot algorithm on top of another distributed algorithm.
 -
 - This version uses only the features provided by the nu-obj calculus
 - and a Scala like syntax on top of it.
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

-- A little utility class
class FaninChannel extends Source {
  val cs :: List[Source]
  
  val c = Channel()
  
  val _ = each(zipWithIndex(cs)) >(i, c')> repeat({ c'.get() >x> c.put((i, x)) }) 

  def get() = c.get()
}
def FaninChannel(cs_ :: List[Channel]) = new FaninChannel { val cs = cs_ }

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

-- A process graph mix-in that adds message timestamps and keeps track
-- of the timestamp at each node.

-- Add a timestamp to the MessageBase class. Compute the timestamp
-- based on the current time in the sending process.
class TimestampMessageBase extends MessageBase {
  val t = proc.updateTime(0)
  def toString() :: String = "" + t
}

-- Add timestamp processing to the ProcessorBase.
class TimestampProcessorBase extends ProcessorBase {
  -- When processing a message first update the timestamp and then
  -- let other classes process.
  def process(i :: Integer, m) = updateTime(m.t) >> super.process(i, m)

  -- Added state and methods
  val currentTimestamp = Ref(0)
  def updateTime(t :: Integer) =
    currentTimestamp := max(t, currentTimestamp? + 1) >>
    currentTimestamp?
    
  def getState() = currentTimestamp? : super.getState()  
}

-- A Logger superposed computation
class LoggingProcessorBase extends ProcessorBase {
  def process(i :: Integer, m) = Println("" + name + " Received " + m.toString() + " on " + i + " in state " + getState()) >> super.process(i, m)  
  def sendMessage(i :: Integer, m) = Println("" + name + " Sending " + m.toString() + " on " + i + " in state " + getState()) >> super.sendMessage(i, m)
  
  def getState() :: List[Top] = [] 
  
  val name = ""
}

  
-- Functions to perform random operations.
def rnd() = Random(100)
def rndwait() = Rwait(Random(500))

-- Small distributed algorithm that can be mixed with TimestampSupport

-- Create a new message time derived from MessageBase.
class RandomNormalMessage extends MessageBase {
  val v :: Integer
  def toString() :: String = "" + v
}

class RandomProcessorBase extends ProcessorBase {
  thisProc #
  val RandomNormalMessage = new { 
    def apply(v_ :: Integer) = new RandomNormalMessage { val proc = thisProc # val v = v_ }
    def unapply(v) = v.v
  }
  
  -- The state of the process (sum of sent messages minus received messages)
  val state = Ref(0)
  
  def getState() = state? : super.getState()

  def process(ci, m) = 
    super.process(ci, m) >> ((m >RandomNormalMessage(v)>
        state := state? - v >> (
          val r = rnd()
          upto(nOutputs) >c> 
            rndwait() >>
            state := state? + r >>
            sendMessage(c, RandomNormalMessage(r)) >>
            stop))
          ; signal)
}

-- Combine RandomMessager with TimestampSupport using manual deep mix-in
class TimestampingRandomMessageBase extends MessageBase with TimestampMessageBase {
}
class TimestampingRandomNormalMessage extends RandomNormalMessage with TimestampingRandomMessageBase {
  def toString() :: String = "" + v + ", " + t -- FIXME: We cannot access these as super[RandomNormalMessage].toString and super[TimestampingRandomMessageBase].toString
}
class TimestampingRandomProcessor extends ChannelProcessorBase with LoggingProcessorBase with TimestampProcessorBase with RandomProcessorBase {
  thisProc #
  val RandomNormalMessage = new { 
    def apply(v_ :: Integer) = new TimestampingRandomNormalMessage { val proc = thisProc # val v = v_ }
    def unapply(v) = v.v -- FIXME: It is currently impossible to refer to thisProc.super.RandomNormalMessage
  }
}
def TimestampingRandomProcessor(name_, inputs_, outputs_) = new TimestampingRandomProcessor { val inputs = inputs_ # val outputs = outputs_ # val name = name_ }


val chans = Table(4, { _ >> Channel() }) 

val proc0 = TimestampingRandomProcessor("A", [chans(0)], [chans(1), chans(2)])
TimestampingRandomProcessor("B", [chans(1)], [chans(3)]) |
TimestampingRandomProcessor("C", [chans(3), chans(2)], [chans(0)]) |
chans(0).put(proc0.RandomNormalMessage(1))

{- TODO:

-- Snapshot support: Unlike timestamping, snapshotting requires adding
-- new messages types, so it requires some additional tricks.
class SnapshotSupport extends ProcessGraph {
  class SnapshotMarker extends MessageBase {
  }

  class ProcessorBase extends super.ProcessorBase {
    def process(ci, m) =
      super.process(ci, m) >> m match {
        case SnapshotMarker() => ??? 
        -- Implementation left out because it's complicated and not
        -- important for this example.
      }
  }
}

class def TimestampingSnapshottingRandomMessager() extends RandomMessager with TimestampSupport with SnapshotSupport with ChannelProcessorGraph {
  -- Make the types concrete classes that have all the bounds
  type Message = MessageBase
  type Processor = ProcessorBase

  -- Mix the nested classes
  class ProcessorBase extends super[TimestampSupport].ProcessorBase 
                         with super[RandomMessager].ProcessorBase 
                         with super[SnapshotSupport].ProcessorBase 
                         with super[ChannelProcessorGraph].ProcessorBase {}
  class NormalMessage extends super.NormalMessage 
                         with super[TimestampSupport].MessageBase {}
  class SnapshotMarker extends super.SnapshotMarker 
                          with super[TimestampSupport].MessageBase {}
  -- This may be avoidable using automatic recursive mix-ins
}
-}
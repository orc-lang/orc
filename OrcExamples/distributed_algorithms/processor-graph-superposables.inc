{------ Superposed computation
 -
 - Several computations that can be superposed on others.
 - 
 - Created by amp in Jan, 2015
 -}

include "processor-graph.inc"

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
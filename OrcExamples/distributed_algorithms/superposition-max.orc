{------ Superposed computation
 - An example of superposing a Lamport clock and a distributed snapshot algorithm on top of another distributed algorithm.
 -
 - $Id$
 -
 - Created by amp in Jan, 2015
 -}

include "processor-graph-superposables.inc"

-- Small distributed algorithm that can be mixed with the superposed computations

def rnd() = Random(100)

-- Create a new message type derived from MessageBase.
class MaxNormalMessage extends MessageBase {
  val v :: Integer
  def toString() :: String = "" + v
}

-- A silly algorithm to compute the maximum that a bunch of processors have
class MaxProcessorBase extends ProcessorBase {
  thisProc #
  val NormalMessage = new { 
    def apply(v_ :: Integer) = new MaxNormalMessage { val proc = thisProc # val v = v_ }
    def unapply(v) = v.v
  }
  
  val initialState :: Integer
  
  -- The state of the process: the max value it has seen
  val state = Ref(initialState)
  
  def getState() = state? : super.getState()

  def process(ci, m) = 
    super.process(ci, m) >> ((m >NormalMessage(v)>
        v /= state? >true> (
          (if v :> state? then state := v else signal) >>
          Rwait(100) >>
          upto(nOutputs) >c> 
            sendMessage(c, NormalMessage(state?)) >>
            stop))
          ; signal)
}

-- Combine RandomMessager with TimestampSupport using manual deep mix-in
class TimestampingMaxMessageBase extends MessageBase with TimestampMessageBase {
}
class TimestampingMaxNormalMessage extends MaxNormalMessage with TimestampingMaxMessageBase {
  def toString() :: String = "" + v + ", " + t -- FIXME: We cannot access these as super[RandomNormalMessage].toString and super[TimestampingRandomMessageBase].toString
}
class TimestampingMaxProcessor extends ChannelProcessorBase with LoggingProcessorBase with TimestampProcessorBase with MaxProcessorBase {
  thisProc #
  val NormalMessage = new { 
    def apply(v_ :: Integer) = new TimestampingMaxNormalMessage { val proc = thisProc # val v = v_ }
    def unapply(v) = v.v -- FIXME: It is currently impossible to refer to thisProc.super.RandomNormalMessage
  }
}
def TimestampingMaxProcessor(name_, inputs_, outputs_, initialState_) = new TimestampingMaxProcessor { val inputs = inputs_ # val outputs = outputs_ # val name = name_ # val initialState = initialState_ }


val chans = Table(8, { _ >> Channel() }) 

val proc0 = TimestampingMaxProcessor("A", [chans(0)], [chans(6), chans(1), chans(2)], 25)
TimestampingMaxProcessor("B", [chans(1)], [chans(3), chans(4)], 60) |
TimestampingMaxProcessor("C", [chans(3), chans(2)], [chans(5)], 70) |
TimestampingMaxProcessor("D", [chans(4), chans(5), chans(7)], [chans(0)], 3) |
TimestampingMaxProcessor("E", [chans(6)], [chans(7)], 55) |
chans(0).put(proc0.NormalMessage(50))


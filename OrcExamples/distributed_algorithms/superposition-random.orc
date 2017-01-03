{------ Superposed computation
 - An example of superposing a Lamport clock and a distributed snapshot algorithm on top of another distributed algorithm.
 -
 - Created by amp in Jan, 2015
 -}

include "processor-graph-superposables.inc"

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


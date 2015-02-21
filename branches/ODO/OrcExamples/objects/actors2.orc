{- actors.orc -- Building an actor using Orc Objects
 -
 - $Id: actors.orc 3424 2015-02-21 21:20:11Z arthur.peters@gmail.com $
 -
 - Created by amp on Feb 20, 2015 1:06:57 PM
 -}
 
-- An actor style mail box where receive can handle messages that are not at the top of the queue.
-- This implementation is VERY slow, but it works.
class MailBox {
  val messages = Ref([])
  val nextID = Ref(0)
  val lock = Semaphore(1)
  val waiting = Semaphore(0)
  
  def add(m) = withLock(lock, { messages := append(messages?, [(nextID?, m)]) >> nextID := nextID? + 1 >> waiting.release() })
  
  {--
    - f should publish if it handled the messages and halt silently otherwise.
    - receive is not concurrent safe since actors are assumed to be interally sequential.
    -}
  def receive(f) = 
    def h((id, m)) = f(m) >r> removeMessage(id) >> r ; h(getNextMessage(id))
    h(getFirstMessage())
  
  def removeMessage(id) = 
    withLock(lock, { 
      messages := filter({ _ >(i, _)> i /= id }, messages?)
    })
    
  def getFirstMessage() = 
    withLock(lock, { messages? }) >first : _> first ; 
      waiting.acquire() >> getFirstMessage()
    
  def getNextMessage(id) =
    def h((id', m) : next : _) if (id' = id) = next
    def h(_ : tail) = h(tail)
    def h([]) = waiting.acquire() >> getNextMessage(id)
    h(withLock(lock, { messages? }))
}
def MailBox() = new MailBox

class ActorBase {
  val mailBox = MailBox()
  def sendMessage(m) = mailBox.add(m)
  def receive(receiver) = mailBox.receive(receiver)
}

class Actor extends ActorBase {
  val a = Ref[Integer](0)
  val b = Ref[Integer](0)
  val c = Ref[Integer](0)

  val _ = repeat({
    receive({ val m = _ #
    	      m >("incr")> (a := a? + 1, b := b? + 1, c := c? + 1) |
    	      m >("read", other)> other.sendMessage((a?, b?, c?)) })
  })
}
def Actor() = new Actor

{|

val flag = Cell()

new ActorBase {
  val count = Ref(0)
  val _ = (
    val o = Actor() #
    (upto(40) >> o.sendMessage("incr") | upto(40) >> o.sendMessage(("read", this))) >> stop ;
    Println("Reading") >>
    repeat({
      receive({ _ >(a, b, c) as t> (if a = b && b = c then count := count? + 1 else Println("Fail " + t)) }) >>
      (if count? >= 40 then Println("success") >> flag := signal else signal)
    })
  )
} >> stop
|
flag?

|}

{-
OUTPUT:
Reading
success
signal
-}
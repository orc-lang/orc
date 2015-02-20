{- actors.orc -- Building an actor using Orc Objects
 -
 - $Id$
 -
 - Created by amp on Feb 20, 2015 1:06:57 PM
 -}
 
-- An actor style mail box where receive can handle messages that are not at the top of the queue.
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
      waiting.acquire() >> this.getFirstMessage()
    
  def getNextMessage(id) =
    def h((id', m) : next : _) if (id' = id) = next
    def h(_ : tail) = h(tail)
    def h([]) = waiting.acquire() >> this.getNextMessage(id)
    h(withLock(lock, { messages? }))
}
def MailBox() = new MailBox

-- A simple actor that merges two messages and prints the result.
class Actor {
  val mailBox = MailBox()
  def sendMessage(m) = mailBox.add(m)
  def receive(receiver) = mailBox.receive(receiver)
  
  val _ = repeat({ -- Body
    Println("Actor") >>
    receive({ _ >("test", v)> Println("Test: " + v) >> v }) >v>
    receive({ _ >("other", v')> Println("Other: " + v + " " + v') })
  })
}
def Actor() = new Actor

-- Use the actor to make sure it does the right things.
val a = Actor()
a.sendMessage(("other", 201)) |
a.sendMessage(("test", 42)) |
a.sendMessage(("test", 43)) |
Rwait(1000) >> a.sendMessage(("other", 202))

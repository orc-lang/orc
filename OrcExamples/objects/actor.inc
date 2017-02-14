{- actor.inc -- A tiny "actor library" in Orc
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
    - receive is not concurrent safe since actors are assumed to be internally sequential.
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

class ActorBase {
  val mailBox = new MailBox
  def sendMessage(m) = mailBox.add(m)
  def receive(receiver) = mailBox.receive(receiver)
}
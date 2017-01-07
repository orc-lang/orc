{- actors.orc -- Building an actor using Orc Objects
 -
 - Created by amp on Feb 20, 2015 1:06:57 PM
 -}
 
-- An actor style mail box where receive can handle messages that are not at the top of the queue.
class def MailBox() :: MailBox {
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

class ActorBase {
  val mailBox = MailBox()
  def sendMessage(m) = mailBox.add(m)
  def receive(receiver) = mailBox.receive(receiver)
}

-- A simple actor that merges two messages and prints the result.
class def Actor(target :: ActorBase) :: Actor extends ActorBase {
  val _ = repeat({ -- Body
    receive({ _ >("test", v)> v }) >v>
    receive({ _ >("other", v')> Println("Other: " + v + " " + v') >> target.sendMessage(("pair", v, v')) })
  })
}

class def SumOfProducts() :: SumOfProducts extends ActorBase {
  val sum = Ref(0)

  val _ = repeat({ -- Body
    receive(lambda(m) =
    	      m >("pair", x, y)> sum := sum? + x * y |
    	      m >("print")> Println("Result: " + sum?))
  })
}

{|
val sum = SumOfProducts()
val a = Actor(sum) #

(
a.sendMessage(("other", 2)) |
a.sendMessage(("test", 1)) |
Rwait(100) >> a.sendMessage(("test", 10)) |
Rwait(700) >> a.sendMessage(("other", 3)) |
Rwait(500) >> sum.sendMessage("print")
) >> stop |
Rwait(1000) >> sum.sendMessage("print") >> Rwait(100)
|}

{-
OUTPUT:
Other: 1 2
Result: 2
Other: 10 3
Result: 32
signal
-}

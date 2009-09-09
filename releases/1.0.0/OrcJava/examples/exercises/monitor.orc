{--
Write a program with two functions: dec and wait.
These functions share a state which is a natural number
(n), initially = 5.  When <code>dec()</code> is called,
decrement n.  When <code>wait()</code> is called, wait until
<code>n = 0</code> and then publish a signal.  Write the program
without using <code>Ref</code>.

Hint: create a single process running in the background which
manages the shared state.
--}

type Callback = lambda() :: Top
type Message = Dec() | Wait(Callback)

val m = Buffer[Message]()

def actor(Integer, List[Callback]) :: Top 
def actor(n, waiters) =
  def on(Message) :: Top
  def on(Dec()) =
    if n = 1 then join(waiters)
    else actor(n-1, waiters)
  def on(Wait(callback)) =
    if n = 0 then callback()
    else actor(n, callback:waiters)
  on(m.get())

def dec() = println("Decrementing") >> m.put(Dec())
def wait() =
  val b = Semaphore(0)
  b.acquire() | m.put(Wait(b.release)) >> stop

  actor(5, []) >> stop
| dec() >> stop
| dec() >> stop
| dec() >> stop
| dec() >> stop
| Rtimer(100) >> dec() >> stop
| wait() >> println("Zero!") >> stop

{-
OUTPUT:
Decrementing
Decrementing
Decrementing
Decrementing
Decrementing
Zero!
-}
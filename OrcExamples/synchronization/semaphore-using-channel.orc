{- semaphore-using-channel.orc
 - 
 - Created by misra on Apr 15, 2010 2:08:35 PM
 -}

def NewSemaphore(n :: Integer) =
  class NewSemaphore {
  val b = Channel[Signal]()
    def acquire() = this.b.get()
    def release() = this.b.put(signal)

  def add(Integer) :: Signal
  def add(0) = signal
    def add(i) = this.release() >> this.add(i-1)

    val _ = this.add(n)
  }
  new NewSemaphore

val s = NewSemaphore(3)

s.acquire() >> s.acquire() >> s.acquire()

{-
OUTPUT:
signal
-}

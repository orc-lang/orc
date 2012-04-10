def class channel() = 
  val ch = Channel[Integer]()
  val chlen = Ref[Integer](0)
  val s = Semaphore(1)
  val _ = Rwait(3000) >> Println("time up in channel!")
  def put(x :: Integer) = 
    s.acquire() >>
    (ch.put(x) >>
    chlen := chlen?+1 >> stop; s.release())
  def get() =
    s.acquire() >> ch.get() >x> 
    (chlen:= chlen?-1 >> stop; s.release()) >> x  
  def len() = 
    s.acquire() >> chlen? >n> s.release() >> n  
  signal

val c = channel()

c.put(1113) >> c.put(2223) >> Println(c.len()) >> 
c.get() >> Println(c.len()) >>stop 

{-
OUTPUT:
2
1
time up in channel!
-}

class channel {
  val ch = Channel[Integer]()
  val chlen = Ref[Integer](0)
  val s = Semaphore(1)
  val _ = Rwait(3000) >> Println("time up in channel!")
  def put(x :: Integer) =
    this.s.acquire() >>
    (this.ch.put(x) >>
    this.chlen := this.chlen?+1 >> stop; this.s.release())
  def get() =
    this.s.acquire() >> this.ch.get() >x>
    (this.chlen := this.chlen?-1 >> stop; this.s.release()) >> x
  def len() =
    this.s.acquire() >> this.chlen? >n> this.s.release() >> n
}

val c = new channel

c.put(1113) >> c.put(2223) >> Println(c.len()) >>
c.get() >> Println(c.len()) >>stop

{-
OUTPUT:
2
1
time up in channel!
-}

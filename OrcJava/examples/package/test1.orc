def {capsule} channel() = 
  val ch = Buffer()
  val chlen = Ref(0)
  val s = Semaphore(1)
  def put(x) = 
    s.acquire() >>
    ch.put(x) >>
    chlen := chlen?+1 >> s.release()
  def get() =
    ch.get() >x> s.acquire() >> 
    chlen:= chlen?-1 >>  s.release() >> x
  def len() = 
    s.acquire() >> chlen? >n> s.release() >> n
  signal

val c = channel()

c.put(1113) >> c.put(2223) >> println(c.len()) >> c.get() >> println(c.len()) >>stop 
 

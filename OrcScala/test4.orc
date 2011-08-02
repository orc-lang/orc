def class Ctr() =
  val x = TRef(0)
  def inc() = x := x? + 1
  def inc2() = (atomic inc(), atomic inc()) >> signal
  def get() = x?
  stop

def burst(width, rate) =
  upto(Random(width)) >> signal
| Rwait(Random(rate)) >> burst(width, rate)

def assert(x, n) if (x = n) = Println("ok") >> stop
def assert(x, _) = Println("Unexpected result: " + x) >> stop

-- upto(10000) >>
burst(10, 10) >>
Ctr() >c> 
(atomic c.inc2(), 
 atomic c.inc2(), 
 atomic c.inc2()) >> 
assert(atomic c.get(), 6)
-- two way branching

val in = Buffer()
val out = Buffer()

def compute(x) = x

def net(in,out) =
  val c = Buffer()
  val c' = Buffer()
  val d = Buffer()
  val d' = Buffer()

  def distr(b) =
    in.get() >x>
    (if b then c.put(x) else d.put(x)) >>
    distr(~b)

  def collect(b) =
    (if b then c'.get() else d'.get())  >x>
    out.put(x) >>
    collect(~b)

  def transduce(p,q) =
    p.get() >x> compute(x) >y> q.put(y) >>
    transduce(p,q)

  {- goal for net() -}
  distr(true) | collect(true) | transduce(c,c') | transduce(d,d')

{- Test -}
 
def output()= out.get() >x> println(x) >> output()
def input(i) = in.put(i) >> input(i+1)

  input(0)
| output()
| net(in,out)

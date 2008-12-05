{-
A simple round-robbin load-balancer which starts two
"compute" servers and routes input alternately to both.
-}

val in = Buffer()
val out = Buffer()

{- Trivial compute function -}
def compute(x) = x

{- Start the network -}
def net(in,out) =
  val c = Buffer()
  val c' = Buffer()
  val d = Buffer()
  val d' = Buffer()

  {- Copy input alternately to c and d -}
  def distr(b) =
    in.get() >x>
    (if b then c.put(x) else d.put(x)) >>
    distr(~b)

  {- Copy output alternately from c' and d' -}
  def collect(b) =
    (if b then c'.get() else d'.get())  >x>
    out.put(x) >>
    collect(~b)

  {- Read from p, compute, and put the result on q -}
  def transduce(p,q) =
    p.get() >x> compute(x) >y> q.put(y) >>
    transduce(p,q)

  {- Goal for net() -}
  distr(true) | collect(true) | transduce(c,c') | transduce(d,d')

{-
Test by feeding sequential numbers
into the network starting at 0 and
printing the output.
-}
 
def output()= out.get() >x> println(x) >> output()
def input(i) = in.put(i) >> input(i+1)

  input(0)
| output()
| net(in,out)

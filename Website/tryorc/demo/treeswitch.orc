{-
A simple round-robbin load-balancer which starts multiple
"compute" servers and routes input alternately to each.
-}

val in = Buffer()
val out = Buffer()

{- Trivial compute function -}
def compute(x) = x

{- Base case: start a single compute server -}
def net(1,in,out) =     
  in.get() >x> compute(x) >y> out.put(y) >>
  net(1,in,out)

{- Inductive case: start n compute servers -}
def net(n,in,out) =
  val c = Buffer()
  val c' = Buffer()
  val d = Buffer()
  val d' = Buffer()
  val (s,t) = (if (n%2 = 0) then (n/2,n/2) else ((n-1)/2, (n+1)/2))

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

  {-
  Goal for net(): start two subnetworks and
  route input to each alternately
  -}
  distr(true) | collect(true) | net(s,c,c') | net(t,d,d')

{-
Test by feeding sequential numbers
into the network starting at 0 and
printing the output.
-}
def output()= out.get() >x> println(x) >> output()
def input(i) = in.put(i) >> input(i+1)

  input(0)
| output()
| net(7,in,out)

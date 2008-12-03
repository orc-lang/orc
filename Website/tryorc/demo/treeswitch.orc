val in = Buffer()
val out = Buffer()

def compute(x) = x

def net(1,in,out) =     
  in.get() >x> compute(x) >y> out.put(y) >>
  net(1,in,out)

def net(n,in,out) =
  val c = Buffer()
  val c' = Buffer()
  val d = Buffer()
  val d' = Buffer()
  val (s,t) = (if (n%2 = 0) then (n/2,n/2) else ((n-1)/2, (n+1)/2))

  def distr(b) =
    in.get() >x>
    (if b then c.put(x) else d.put(x)) >>
    distr(~b)

  def collect(b) =
    (if b then c'.get() else d'.get())  >x>
    out.put(x) >>
    collect(~b)

  {- goal for net() -}
  distr(true) | collect(true) | net(s,c,c') | net(t,d,d')

{- Test -}
def output()= out.get() >x> println(x) >> output()
def input(i) = in.put(i) >> input(i+1)

  input(0)
| output()
| net(7,in,out)

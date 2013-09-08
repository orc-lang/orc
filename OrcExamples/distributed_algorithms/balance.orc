{- balance.orc

EXERCISE:

Write a function which takes an input channel (in),
an output channel (out), and a list of sites (ps)
as arguments.  The function must repeatedly read an
input value from in, call one of the sites in ps
with the value (using each site in the list in turn),
and write the result to out.  The order values are
written to the output channel must correspond to the
order values were received on the input channel.

SOLUTION:
--}

type InType = Number
type OutType = Number
type SiteType = (lambda(InType) :: OutType)

def balance(Channel[InType], Channel[OutType], List[SiteType]) :: Bot
def balance(in, out, ps) =
  def makeChannel(_ :: Top) = Channel[OutType]()
  val cs = map(makeChannel, ps)
  def write(List[Channel[OutType]]) :: Bot
  def read(List[(SiteType,Channel[OutType])]) :: Bot
  def write(c:cs) = out.put(c.get()) >> write(append(cs, [c]))
  def read((p,c):pcs) =
    ( in.get() ; c.close() >> stop ) >x>
    ( c.put(p(x)) >> stop | read(append(pcs, [(p,c)])) )
  write(cs) | read(zip(ps,cs))

val in = Channel[InType]()
val out = Channel[OutType]()
def compute(Integer) :: SiteType
def compute(n) = lambda(x) = Println("Site " + n + " computing") >> x*x  #


  ( balance(in, out, [compute(1), compute(2), compute(3), compute(4)])
    ; out.close() >> stop )
| ( upto(10) >n> in.put(n) >> stop
    ; in.close() >> stop )
| repeat(out.get)

{-
OUTPUT:PERMUTABLE:
Site 1 computing
0
Site 2 computing
1
Site 3 computing
4
Site 4 computing
9
Site 1 computing
16
Site 2 computing
25
Site 3 computing
36
Site 4 computing
49
Site 1 computing
64
Site 2 computing
81
-}

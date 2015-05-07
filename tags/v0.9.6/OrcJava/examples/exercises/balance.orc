{--
Write a function which takes an input channel (in),
an output channel (out), and a list of sites (ps)
as arguments.  The function must repeatedly read an
input value from in, call one of the sites in ps
with the value (using each site in the list in turn),
and write the result to out.  The order values are
written to the output channel must correspond to the
order values were received on the input channel.
--}

def balance(in, out, ps) =
  val bs = map(ignore(Buffer), ps)
  def write(b:bs) = out.put(b.get()) >> write(append(bs, [b]))
  def read((p,b):pbs) =
    ( in.get() ; b.close() >> stop ) >x>
    ( b.put(p(x)) >> stop | read(append(pbs, [(p,b)])) )
  write(bs) | read(zip(ps,bs))

val in = Buffer()
val out = Buffer()
def compute(n)(x) = println("Site " + n) >> x*x

( balance(in, out, [compute(1), compute(2), compute(3), compute(4)])
  ; out.close() >> stop )
| ( upto(10) >n> in.put(n) >> stop
    ; in.close() >> stop )
| repeat(out.get)

{-
OUTPUT:
Site 1
0
Site 2
1
Site 3
4
Site 4
9
Site 1
16
Site 2
25
Site 3
36
Site 4
49
Site 1
64
Site 2
81
-}
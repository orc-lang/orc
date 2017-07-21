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

include "benchmark.inc"

type InType = Number
type OutType = Number
type SiteType = (lambda(InType) :: OutType)

def uptoSeq(n :: Integer, f :: lambda(Integer) :: Signal) :: Signal =
  def iter(Integer) :: Signal
  def iter(i) if (i <: n) = f(i) >> iter(i+1)
  def iter(_) = signal
  iter(0)

def run(i) =
    --Println("Run " + i) >>
    (
    def balance(Channel[InType], Channel[OutType], List[SiteType]) :: Bot
    def balance(in, out, ps) =
      def makeChannel(_ :: Top) = Channel[OutType]()
      val cs = map(makeChannel, ps)
      
      def write(List[Channel[OutType]]) :: Bot
      def read(List[(SiteType,Channel[OutType])]) :: Bot
      
      def write(c:cs) = out.put(c.get()) >> write(append(cs, [c]))
      
      def read((p,c):pcs, isPrevDone) =
        ( in.get() ; isPrevDone >> c.close() >> stop ) >x>
        ( val isPrevDone' = (c.put(p(x)), isPrevDone) >> signal # read(append(pcs, [(p,c)]), isPrevDone') )
      
      write(cs) | read(zip(ps,cs), signal)
    
    val in = Channel[InType]()
    val out = Channel[OutType]()
    def compute(Integer) :: SiteType
    def compute(n) = { _ >x> {-Println("Site " + n + " computing") >>-} (x, x*x) } #
    
    
      ( balance(in, out, [compute(1), compute(2), compute(3), compute(4)])
        ; out.close() >> stop )
    | ( uptoSeq(10, in.put) >> stop
        ; in.close() >> stop )
    | collect({ repeat(out.get) }) >x> (if length(x) /= 10 then Println("FAIL " + length(x)) >> stop else stop)
    ) ; signal
    
benchmark({
  uptoSeq(200, run)
})

{-
BENCHMARK
-}

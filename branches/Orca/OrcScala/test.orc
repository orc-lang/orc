def inc(x) = atomic (x := x? + 1)

def report(n) =
  def show(1000) = "0"
  def show(1001) = "1"
  def show(1002) = "2"
  def show(1003) = "_"
  Print(show(n)) >> stop


upto(500) >>
  TRef() >t> 
    atomic (t := 1000) >> 
      (inc(t), inc(t), inc(t)) >> 
        atomic( t? ) >r> 
          report(r)

{-
def randabort() = Ift(Random(2) = 0) >> Abort()

atomic ( Println("txn initiated") >> 1 | Rwait(2000) >> 2 | Rwait(1000) >> randabort())
-}
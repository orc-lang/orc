def inc(x) = atomic (x := x? + 1)

TRef() >t> atomic (t := 0) >> (inc(t), inc(t), inc(t)) >> atomic( t? )

{-
def randabort() = Ift(Random(2) = 0) >> Abort()

atomic ( Println("txn initiated") >> 1 | Rwait(2000) >> 2 | Rwait(1000) >> randabort())
-}

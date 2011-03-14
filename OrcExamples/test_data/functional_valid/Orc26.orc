

val c = Cell()

Rwait(1000) >> c.write(10) >> Vwait(2) >> Vclock()
| c.read() >> Vwait(3) >> Vclock()

{-
OUTPUT:
2
3
-}



val c = Cell()

Rwait(1000) >> c.write(10) >> Vtimer(2) >> Vclock()
| c.read() >> Vtimer(3) >> Vclock()

{-
OUTPUT:
2
3
-}

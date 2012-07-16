{- Orc26.orc -- Simple test for Orc virtual time
 - 
 - $Id$
 - 
 - Created by amshali
 -}

val c = Cell[Integer]()

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Rwait(1000) >> c.write(10) >> Vawait(2) >> VtimeI()
| c.read() >> Vawait(3) >> VtimeI()
)

{-
OUTPUT:
2
3
-}

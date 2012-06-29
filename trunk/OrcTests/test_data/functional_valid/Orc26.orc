{- Orc26.orc -- Simple test for Orc virtual time
 - 
 - $Id$
 - 
 - Created by amshali
 -}

val c = Cell()

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Rwait(1000) >> c.write(10) >> Vawait(2) >> Vtime()
| c.read() >> Vawait(3) >> Vtime()
)

{-
OUTPUT:
2
3
-}

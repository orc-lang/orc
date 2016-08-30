{- Orc24.orc -- Simple test for Orc virtual time
 - 
 - $Id$
 - 
 - Created by amshali
 -}

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Rwait(500) >> Vawait(2) >> VtimeI() >x> Println("500:"+x) >> stop
| Println("test") >> VtimeI()
| Rwait(1000) >> Vawait(5)  >> VtimeI() >x> Println("1000:"+x) >> stop
)

{-
--OUTPUT:
test
0
500:2
1000:5
-}

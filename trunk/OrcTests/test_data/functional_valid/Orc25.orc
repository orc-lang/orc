{- Orc25.orc -- Simple test for Orc virtual time
 - 
 - $Id$
 - 
 - Created by amshali
 -}

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Rwait(999) >> Vawait(VtimeI()+2) >> VtimeI() >x> Println("999:"+x) >> stop
| Println("test") >> VtimeI()
| Rwait(1000) >> Vawait(VtimeI()+3)  >> VtimeI() >x> Println("1000:"+x) >> stop
)

{-
OUTPUT:
test
0
999:2
1000:5
-}
{-
OUTPUT:
test
0
999:2
1000:3
-}

{- Orc25.orc -- Simple test for Orc virtual time
 - 
 - Created by amshali
 -}

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Rwait(900) >> Vawait(VtimeI()+2) >> VtimeI() >x> Println("900:"+x) >> stop
| Println("test") >> Println(VtimeI()) >> stop
| Rwait(1000) >> Vawait(VtimeI()+3)  >> VtimeI() >x> Println("1000:"+x) >> stop
)

{-
OUTPUT:
test
0
900:2
1000:5
-}

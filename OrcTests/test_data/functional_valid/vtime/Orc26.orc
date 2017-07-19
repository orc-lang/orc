{- Orc26.orc -- Simple test for Orc virtual time
 - 
 - Created by amshali
 -}

val c = Cell[Integer]()

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Rwait(1000) >> c.write(10) >> Vawait(2) >> Println(VtimeI())
| c.read() >> Vawait(3) >> Println(VtimeI())
) >> stop

{-
OUTPUT:
2
3
-}

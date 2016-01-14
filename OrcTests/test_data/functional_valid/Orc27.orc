{- Orc27.orc -- Simple test for Orc virtual time
 - 
 - Created by amshali
 -}

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>
((Rwait(100) >> x >> VtimeI()) <x< (Vawait(3) | Vawait(2)))

{-
OUTPUT:
2
-}

{- Orc27.orc -- Simple test for Orc virtual time
 - 
 - Created by amshali
 -}

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>

(
  val x = {| Vawait(3) | Vawait(2) |} 
  Rwait(100) >> x >> VtimeI()
)
-- TODO: This test case assumes Rwait is non-quiesent. This may not be the case.

{-
OUTPUT:
2
-}

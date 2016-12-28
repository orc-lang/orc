{- Orc41.orc -- Orc program Orc41
 - 
 - Created by dkitchin on Dec 6, 2012 3:47:25 PM
 -}

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>
(
  val x = {| Vawait(3) | Vawait(2) |} 
  x >> VtimeI()
)

{-
OUTPUT:
2
-}

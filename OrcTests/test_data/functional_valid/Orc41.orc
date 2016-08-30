{- Orc41.orc -- Orc program Orc41
 - 
 - $Id$
 - 
 - Created by dkitchin on Dec 6, 2012 3:47:25 PM
 -}

def VtimeI() = (Vtime() :!: Integer)

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( (x >> VtimeI()) <x< (Vawait(3) | Vawait(2)) )

{-
--OUTPUT:
2
-}

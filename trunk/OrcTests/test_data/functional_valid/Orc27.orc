{- Orc27.orc -- Simple test for Orc virtual time
 - 
 - $Id$
 - 
 - Created by amshali
 -}

Vclock(IntegerTimeOrder) >> Vawait(0) >>
(Rwait(100) >> Vtime()) << (Vawait(3) | Vawait(2))

{-
OUTPUT:
2
-}

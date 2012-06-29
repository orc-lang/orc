{- Orc24.orc -- Simple test for Orc virtual time
 - 
 - $Id$
 - 
 - Created by amshali
 -}

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Rwait(500) >> Vawait(2) >> Vtime() >x> Println("500:"+x) >> stop
| Println("test") >> Vtime()
| Rwait(1000) >> Vawait(3)  >> Vtime() >x> Println("1000:"+x) >> stop
)

{-
OUTPUT:
test
0
500:2
1000:5
-}

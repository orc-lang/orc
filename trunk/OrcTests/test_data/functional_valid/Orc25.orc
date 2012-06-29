{- Orc25.orc -- Simple test for Orc virtual time
 - 
 - $Id$
 - 
 - Created by amshali
 -}

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Rwait(999) >> Vawait(Vtime()+2) >> Vtime() >x> Println("999:"+x) >> stop
| Println("test") >> Vtime()
| Rwait(1000) >> Vawait(Vtime()+3)  >> Vtime() >x> Println("1000:"+x) >> stop
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

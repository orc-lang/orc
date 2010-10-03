{-
Added by amshali
-}
Rtimer(999) >> Vtimer(2) >> Vclock() >x> println("999:"+x) >> stop
| println("test") >> Vclock()
| Rtimer(1000) >> Vtimer(3)  >> Vclock() >x> println("1000:"+x) >> stop


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
1000:3
999:5
-}

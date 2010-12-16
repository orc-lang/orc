{-
Added by amshali
-}
Rtimer(500) >> Vtimer(2) >> Vclock() >x> println("500:"+x) >> stop
| println("test") >> Vclock()
| Rtimer(1000) >> Vtimer(3)  >> Vclock() >x> println("1000:"+x) >> stop


{-
OUTPUT:
test
0
500:2
1000:5
-}

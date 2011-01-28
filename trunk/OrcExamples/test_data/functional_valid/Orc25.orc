{-
Added by amshali
-}
Rtimer(999) >> Vtimer(2) >> Vclock() >x> Println("999:"+x) >> stop
| Println("test") >> Vclock()
| Rtimer(1000) >> Vtimer(3)  >> Vclock() >x> Println("1000:"+x) >> stop


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

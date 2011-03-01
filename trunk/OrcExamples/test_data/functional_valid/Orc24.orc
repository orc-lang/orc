{-
Added by amshali
-}
Rwait(500) >> Vtimer(2) >> Vclock() >x> Println("500:"+x) >> stop
| Println("test") >> Vclock()
| Rwait(1000) >> Vtimer(3)  >> Vclock() >x> Println("1000:"+x) >> stop


{-
OUTPUT:
test
0
500:2
1000:5
-}

{-
Added by amshali
-}
Rwait(999) >> Vwait(2) >> Vclock() >x> Println("999:"+x) >> stop
| Println("test") >> Vclock()
| Rwait(1000) >> Vwait(3)  >> Vclock() >x> Println("1000:"+x) >> stop


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

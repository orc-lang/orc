def Countdown(i) = if(i > 0) >> 
				   ( i 
				   | Rtimer(500) >> Countdown(i-1) 
				   )

Countdown(5)
{-
OUTPUT:
5
4
3
2
1
-}
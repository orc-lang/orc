def Countdown(Integer) :: Integer
def Countdown(i) = IfT(i :> 0) >>
				   ( i
				   | Rtimer(10) >> Countdown(i-1)
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

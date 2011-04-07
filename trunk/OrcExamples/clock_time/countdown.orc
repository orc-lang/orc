def Countdown(Integer) :: Integer
def Countdown(i) = Ift(i :> 0) >>
				   ( i
				   | Rwait(10) >> Countdown(i-1)
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

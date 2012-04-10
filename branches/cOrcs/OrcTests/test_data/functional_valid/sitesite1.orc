{- 
  An Orc site is protected from termination,
  and will keep its host execution alive.
-}

def countdown(Integer) :: Signal
def countdown(0) = signal
def countdown(i) = Println(i) >> Rwait(500) >> countdown(i-1)
val Countdown = MakeSite(countdown)

Let(Rwait(250) >> true | Countdown(4)) -- | Rwait(750) >> false
 

{-
OUTPUT:
4
true
3
2
1
-}

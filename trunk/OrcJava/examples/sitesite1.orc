{- 
  An Orc site is protected from termination,
  and will keep its host execution alive.
-}

def countdown(Integer) :: Signal
def countdown(0) = signal
def countdown(i) = println(i) >> Rtimer(500) >> countdown(i-1)
val Countdown = Site(countdown)

let(Rtimer(250) >> true | Countdown(4)) | Rtimer(750) >> false
 

{-
OUTPUT:
4
true
3
false
2
1
-}
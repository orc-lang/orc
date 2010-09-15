{-
This file tests the behavior of engine when we have so many long running sites.
-}

upto(50) >> Delay(3000)>> stop
| Delay(2000) >> "delay"
| "immediate"
| Rtimer(500) >> println("rtimer") >> stop

{-
OUTPUT:
"immediate"
rtimer
"delay"
-}

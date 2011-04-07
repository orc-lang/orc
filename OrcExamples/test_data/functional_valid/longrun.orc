{-
This file tests the behavior of engine when we have so many long running sites.
-}

import class Thread = "java.lang.Thread"
def delay(n :: Integer) = Thread.sleep(n)

upto(50) >> delay(3000)>> stop
| delay(2000) >> "delay"
| "immediate"
| Rwait(500) >> Println("rtimer") >> stop

{-
OUTPUT:
"immediate"
rtimer
"delay"
-}

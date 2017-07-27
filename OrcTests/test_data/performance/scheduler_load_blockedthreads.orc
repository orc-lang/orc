{- scheduler_load_blockedthreads.orc -- Orc program scheduler_load_blockedthreads
 - 
 - Created by jthywiss on Mar 31, 2011 8:30:17 PM
 -}

include "benchmark.inc"

import class Thread = "java.lang.Thread"

def sleep(x) = Thread.sleep(x)

val timestep = 100 -- ms

def snoozie(x) if (x :> 0) = sleep(timestep) >> snoozie(x - timestep)

def f(x) if (x :> 0) = (f(x-1) >> snoozie(500)) | snoozie(500)

benchmark({ f(1000) })

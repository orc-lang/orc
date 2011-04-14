{- scheduler_load_blockedthreads.orc -- Orc program scheduler_load_blockedthreads
 - 
 - $Id: scheduler_load_blockedthreads.orc 2721 2011-04-07 02:35:57Z jthywissen $
 - 
 - Created by jthywiss on Mar 31, 2011 8:30:17 PM
 -}

import class Thread = "java.lang.Thread"

def sleep(x) = Thread.currentThread().sleep(x)

val timestep = 100 -- ms

def snoozie(x) if (x :> 0) = sleep(timestep) >> snoozie(x - timestep)

def f(x) if (x :> 0) = (f(x-1) >> snoozie(500)) | snoozie(500)

f(1000)

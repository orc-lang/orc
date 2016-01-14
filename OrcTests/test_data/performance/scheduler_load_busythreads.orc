{- scheduler_load_busythreads.orc -- Orc program scheduler_load_busythreads
 - 
 - Created by jthywiss on Mar 31, 2011 8:32:56 PM
 -}


def spin(x) if (x :> 0) = signal >> signal >> signal >> signal >> signal >> signal >> signal >> spin(x - 1)

def f(x) if (x :> 0) = f(x-1) | spin(500) | spin(500)

f(500)

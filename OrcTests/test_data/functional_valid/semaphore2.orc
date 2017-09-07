{- semaphore2.orc
 - 
 - Ensure that negative-valued semaphores can't be created.
 - 
 - Created by dkitchin on Feb 20, 2012
 -}

Semaphore(-1) >> false ; true

{-
OUTPUT:
Error: java.lang.IllegalArgumentException: Semaphore requires a non-negative argument
true
-}

{-
OUTPUT:
Error: orc.error.runtime.JavaException: java.lang.IllegalArgumentException: Semaphore requires a non-negative argument
true
-}

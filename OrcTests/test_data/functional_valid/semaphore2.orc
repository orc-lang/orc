{- semaphore2.orc
 - 
 - Ensure that negative valued semaphores can't be created,
 - and that attempting to create one causes Semaphore to
 - halt silently.
 - 
 - Created by dkitchin on Feb 20, 2012
 -}

Semaphore(-1) >> false ; true

{-
OUTPUT:
true
-}

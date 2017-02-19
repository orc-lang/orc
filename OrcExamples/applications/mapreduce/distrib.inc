{- distrib.inc -- A simple distributed execution control library for the MapReduce implementations.
 - 
 - This should be implementable on top of dOrc using strategic use of policy sets.
 -}

{-
Publish signal at a location which has d or will receive a copy once d is resolved.

This should not force d.
-}
def at(d :: Top) :: Signal = signal

 
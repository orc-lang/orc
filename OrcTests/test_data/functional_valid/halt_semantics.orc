{- halt_semantics.orc -- Orc program halt_semantics
 - 
 - $Id$
 - 
 - Created by dkitchin on Jan 23, 2013 2:27:00 AM
 -}

def blockForever() = Channel[Top]().get()

val result = (
  val x = blockForever()
  val y = stop
  x+y ; y+x ; true
)
result

{-
OUTPUT:
true
-}
{- Cor37.orc
 -
 - Test for Cor records
 -
 - Created by dkitchin on Jul 8, 2010 13:22:00 AM
 -}

val empty = {.  .} :: {.  .}
val one = {. x = 1 .} :: {. x :: Integer .} 
val two = {. y = 3, z = true .} :: {. y :: Integer, z :: Boolean .} 

  empty
| one
| (one.x :: Integer)
| (two.y :: Integer)
| (two.z :: Boolean)
 

{-
OUTPUT:PERMUTABLE:
{.  .}
{. x = 1 .}
1
3
true
-}

{- Orc31.orc
 - 
 - Testing of user-defined pattern matching via unapply
 - 
 - Created by dkitchin on Dec 12, 2010
 -}

-- Brute force search for a cube root
def findCube(Integer, Integer) :: Integer
def findCube(i, x) if (i*i*i <: x) = findCube(i+1, x)
def findCube(i, x) if (i*i*i = x) = i
def findCube(i, x) if (i*i*i :> x) = stop {- unneeded, but helps improve readability -}

val cubeOf = {. unapply = lambda (x :: Integer) = findCube(0,x) .}

signal >> (63|64|65) >cubeOf(i)> i

{-
OUTPUT:
4
-}

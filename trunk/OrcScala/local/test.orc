{-
-- Brute force search for a cube root
def findCube(i, x) if (i*i*i <: x) = findCube(i+1, x)
def findCube(i, x) if (i*i*i = x) = i

val cubeOf = {. unapply = lambda (x) = findCube(0,x) .}

signal >> (63|64|65) >cubeOf(i)> i

-}

signal
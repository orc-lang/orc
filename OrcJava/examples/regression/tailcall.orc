{-
This program doesn't do anything interesting, but it does
test a regression we had once related to tail calls.
-}

def x(xs) = each(xs)
x([1,2,3])
{-
OUTPUT:
1
2
3
-}
{--
Write a function which, given a list, returns
the number of elements in the list.
--}

def length(List[Top]) :: Integer
def length([]) = 0
def length(x:xs) = 1 + length(xs)

length([0,0,0,0])

{-
OUTPUT:
4
-}

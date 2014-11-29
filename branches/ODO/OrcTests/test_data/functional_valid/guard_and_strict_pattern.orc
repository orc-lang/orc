def f(Integer) :: Integer
def f(0) if (true) = 0
def f(x) = x+1 #

def g(List[Integer]) :: Integer
def g(x:xs) if (true) = x

f(0) | g([1,2])

{-
OUTPUT:PERMUTABLE:
0
1
-}

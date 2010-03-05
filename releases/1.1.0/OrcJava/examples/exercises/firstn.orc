{--
Write a function which, given a number (n) and a channel (c),
returns a list of the first n values received from c.
--}

def firstN[A](Integer, Buffer[A]) :: List[A]
def firstN(0, c) = []
def firstN(n, c) = c.get() >x> x:firstN(n-1, c)

val c = Buffer[Integer]()
  firstN(5, c)
| upto(10) >n> c.put(n) >> stop

{-
OUTPUT:
[0, 1, 2, 3, 4]
-}
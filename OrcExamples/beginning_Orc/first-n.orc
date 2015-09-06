{- first-n.orc

EXERCISE:

Write a function which, given a number (n) and a channel (c),
returns a list of the first n values received from c.

SOLUTION:
-}

def firstN[A](Integer, Channel[A]) :: List[A]
def firstN(0, c) = []
def firstN(n, c) = c.get() >x> x:firstN(n-1, c)

def putn(Channel[Integer], Integer) :: Bot 
def putn(c, n) =
  def putni(Channel[Integer], Integer, Integer) :: Bot 
  def putni(c, n, i) = if (i <: n) then c.put(i) >> putni(c, n, i+1) else stop
  putni(c, n, 0)

val c = Channel[Integer]()
  firstN(5, c)
| putn(c, 10) >> stop

{-
OUTPUT:
[0, 1, 2, 3, 4]
-}

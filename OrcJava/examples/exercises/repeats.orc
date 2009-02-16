{--
Write a function which, given a list of natural numbers,
returns the list with each element x replaced by x
occurrences of the same value.  For example, given the
list <code>[1,2,1,4]</code>, the function returns
<code>[1,2,2,1,4,4,4,4]</code>.
--}


def repeats(list) =
  def loop(_, 0, []) = []
  def loop(_, 0, x:xs) = loop(x, x, xs)
  def loop(x, n, xs) = x:loop(x, n-1, xs)
  loop(0, 0, list)

repeats([1,2,1,4])

{-
OUTPUT:
[1, 2, 2, 1, 4, 4, 4, 4]
-}

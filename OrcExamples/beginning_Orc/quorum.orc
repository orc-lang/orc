{- quorum.orc

EXERCISE:

Write a function which, given a number (n) and a
list of sites, calls every site in the list in
parallel and returns a list of the first n responses.
The order of items in the returned list is unimportant.

SOLUTION::
--}

{- Get the first n items from channel c -}
def firstN[A](Integer, Channel[A]) :: List[A]
def firstN(0, c) = []
def firstN(n, c) = c.get() >x> x:firstN(n-1, c)

{- Return a list of the first n responses from sites -}
def quorum[A](Integer, List[lambda() :: A]) :: List[A]
def quorum(n, sites) =
  val c = Channel[A]()
  firstN(n, c) | each(sites) >s> c.put(s()) >> stop

{- Demo/Test -}
def example(n :: Integer) = { Rwait(n * 10) >> n }
quorum(3, [example(0), example(10), example(20), example(30), example(40)])

{-
OUTPUT:
[0, 10, 20]
-}

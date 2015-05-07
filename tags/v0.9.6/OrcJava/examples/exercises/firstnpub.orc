{--
Write a program which calls a definition f, which
may publish multiple values. Publish the first 10
values published by <code>f()</code>, and then
terminate the call to f.
--}

{- Publish first n values received on c, then release s -}
def allow(0, c, s) = c.closenb() >> s.release() >> stop
def allow(n, c, s) = c.get() >x> ( x | allow(n-1, c, s) )

{- Example f -}
def f() = upto(10) >x> Rtimer(x) >> x

{- Main program -}
val c = Buffer()
val s = Semaphore(0)
allow(5, c, s) <<
  s.acquire() | f() >x> c.put(x) >> stop
  
{-
OUTPUT:
0
1
2
3
4
-}
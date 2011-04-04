{--
Write a program which calls a definition f, which
may publish multiple values. Publish the first 5
values published by <code>f()</code>, and then
terminate the call to f.
--}

{- Publish first n values received on c, then release s -}
def allow[A](Integer, Channel[A], Semaphore) :: A
def allow(0, c, s) = c.closeD() >> s.release() >> stop
def allow(n, c, s) = c.get() >x> ( x | allow(n-1, c, s) )

{- Example f -}
def f() = upto(10) >x> Rwait(x * 100) >> x

{- Main program -}
val c = Channel[Integer]()
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
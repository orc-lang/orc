{--
Write a program which calls a definition f, which
may publish multiple values. Publish all of the
values published by {{f()}} within 100ms, and
then terminate the call to f.
--}

{- Example f -}
def f() = upto(10) >n> Rwait(n*20) >> n

{- Main program -}
val c = Channel[Integer]()
repeat(c.get) <<
    f() >x> c.put(x) >> stop
  | Rwait(100) >> c.closeD()


{- 
  Note: 5 may or may not be present in the output;
  f publishes 5, and f is killed, simultaneously
  at 100ms
-}

{-
OUTPUT:
0
1
2
3
4
-}
{-
OUTPUT:
0
1
2
3
4
5
-}
{--
Write a program which calls a definition f, which
may publish multiple values. Publish all of the
values published by {{f()}} within 1 second, and
then terminate the call to f.
--}

{- Example f -}
def f() = upto(10) >n> Rtimer(n*20) >> n

{- Main program -}
val c = Buffer[Integer]()
repeat(c.get) <<
    f() >x> c.put(x) >> stop
  | Rtimer(100) >> c.closenb()

{-
OUTPUT:
0
1
2
3
4
-}
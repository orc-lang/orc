val c = Buffer()
c.get() | Rtimer(3000) >> c.put(3) >> stop

{-
OUTPUT:
3
-}

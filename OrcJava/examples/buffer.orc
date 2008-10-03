val c = Buffer()
c.get()
| c.get()
| Rtimer(1000) >>
  c.put(3) >>
  c.close() >>
  stop

{-
OUTPUT:
3
-}

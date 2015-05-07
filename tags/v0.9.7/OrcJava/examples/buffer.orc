val c = Buffer[Integer]()
c.get()
| Rtimer(200) >> c.get()
| Rtimer(200) >> c.get()
| Rtimer(100) >>
  c.put(3) >>
  c.put(3) >>
  c.close()

{-
OUTPUT:
3
3
signal
-}

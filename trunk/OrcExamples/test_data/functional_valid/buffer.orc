val c = Buffer[Integer]()
c.get()
| Rwait(200) >> c.get()
| Rwait(200) >> c.get()
| Rwait(100) >>
  c.put(3) >>
  c.put(3) >>
  c.close()

{-
OUTPUT:
3
3
signal
-}

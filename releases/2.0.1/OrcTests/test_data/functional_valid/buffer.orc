val c = Channel[Integer]()
c.get()
| Rwait(200) >> c.get()
| Rwait(200) >> c.get()
| Rwait(100) >>
  c.put(3) >>
  c.put(3) >>
  c.close() >> 
  Rwait(50) >>
  4

{-
OUTPUT:
3
3
4
-}

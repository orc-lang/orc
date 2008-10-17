val c = BoundedBuffer(1)
  c.put(1) >> "Put " + 1
| c.put(2) >> "Put " + 2
| Rtimer(100) >> (
    c.get() >n> "Got " + n
  | c.get() >n> "Got " + n
  )

{-
OUTPUT:
Put 1
Put 2
Got 1
Put 3
Got 2
Put 4
Got 3
Put 5
Got 4
Got 5
-}
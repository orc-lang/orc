val c = BoundedChannel[Integer](2)
  for(1, 6) >i> c.put(i) >> "Put " + i
| Rwait(100) >> (
    for(1, 6) >> c.get() >i> "Got " + i
  )

{-
OUTPUT:PERMUTABLE:
"Put 1"
"Put 2"
"Got 1"
"Put 3"
"Got 2"
"Put 4"
"Got 3"
"Put 5"
"Got 4"
"Got 5"
-}

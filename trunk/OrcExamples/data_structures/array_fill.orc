-- Three different ways to prefill arrays:
-- An immutable array with the value equal to the index
val a = Table[Integer](3, Let)
-- A mutable array with the value equal to the index
val b = 
  Array[Integer](5) >a>
  fillArray[Integer](a, Let) >>
  a
-- A mutable array initialized to constant value 0
val c = 
  Array[Integer](3) >a>
  fill(a, 0) >>
  a

-- Publish the values of each array in
-- a predictable sequence
val pubs = Channel[Integer]()

signal >>
(
  pubs.put(a(0)) >> stop
  ; pubs.put(a(1)) >> stop
  ; pubs.put(a(2)) >> stop
  ; each(arrayToList(b)) >x> pubs.put(x) >> stop
  ; each(arrayToList(c)) >x> pubs.put(x) >> stop
  ; pubs.close() >> stop
) | repeat(pubs.get)

{-
OUTPUT:PERMUTABLE:
0
1
2
0
1
2
3
4
0
0
0
-}

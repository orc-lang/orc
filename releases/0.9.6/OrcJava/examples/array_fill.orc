-- Three different ways to prefill arrays:
-- An immutable array with the value equal to the index
val a = IArray(3, let)
-- A mutable array with the value equal to the index
val b =
  Array(5) >a>
  fillArray(a, let) >>
  a
-- A mutable array initialized to constant value 0
val c =
  Array(3) >a>
  a.fill(0) >>
  a

-- Publish the values of each array in
-- a predictable sequence
val pubs = Buffer()
(
  (a(0) | a(1) | a(2)) >x> pubs.put(x) >> stop
  ; each(b) >x> pubs.put(x) >> stop
  ; each(c) >x> pubs.put(x) >> stop
  ; pubs.close() >> stop
) | repeat(pubs.get)
{-
OUTPUT:
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
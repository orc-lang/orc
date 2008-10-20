-- Two different ways to prefill arrays
val a = IArray(3, let)
val b =
  Array(5) >a>
  fillArray(a, let) >>
  a
a(0) | a(1) | a(2) | each(b)
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
-}
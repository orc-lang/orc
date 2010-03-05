val a = Array[Integer](10)
a(1) := 1 >>
a(2) := 2 >>
a(3) := 3 >>
a.slice(1,4) >b>
each(b)
{-
OUTPUT:
1
2
3
-}
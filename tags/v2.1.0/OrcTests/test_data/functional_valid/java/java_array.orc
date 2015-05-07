val a = Array[Integer](10)

a(1) := 1 >>
a(2) := 2 >>
a(3) := 3 >>
sliceArray(a, 1, 4) >b>
each(arrayToList(b))
{-
OUTPUT:PERMUTABLE
1
2
3
-}

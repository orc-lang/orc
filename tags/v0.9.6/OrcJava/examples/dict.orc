val d = Dictionary()
  println(d.one?) >>
  println(d.two?) >>
  stop
| d.one := 1 >>
  d.two := 2 >>
  stop
{-
OUTPUT:
1
2
-}
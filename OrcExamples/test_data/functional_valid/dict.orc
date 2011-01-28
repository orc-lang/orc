val d = Dictionary()
  Println(d.one?) >>
  Println(d.two?) >>
  stop
| d.one := 1 >>
  d.two := 2 >>
  stop
{-
OUTPUT:
1
2
-}
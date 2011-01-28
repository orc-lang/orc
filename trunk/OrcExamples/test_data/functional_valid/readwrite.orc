  Vtimer(0) >> Write(1)
| Vtimer(1) >> Write((3.0, []))
| Vtimer(2) >> Write("hi")
| Vtimer(3) >> Read("1")
| Vtimer(4) >> Read("(3.0, [])")
| Vtimer(5) >> Read("\"hi\"")

{-
OUTPUT:
"1"
"(3.0, [])"
"\"hi\""
1
(3.0, [])
"hi"
-}
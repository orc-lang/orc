  Vtimer(0) >> write(1)
| Vtimer(1) >> write((3.0, []))
| Vtimer(2) >> write("hi")
| Vtimer(3) >> read("1")
| Vtimer(4) >> read("(3.0, [])")
| Vtimer(5) >> read("\"hi\"")

{-
OUTPUT:
"1"
"(3.0, [])"
"\"hi\""
1
(3.0, [])
"hi"
-}
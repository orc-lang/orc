  Ltimer(0) >> write(1)
| Ltimer(1) >> write((3.0, []))
| Ltimer(2) >> write("hi")
| Ltimer(3) >> read("1")
| Ltimer(4) >> read("(3.0, [])")
| Ltimer(5) >> read("\"hi\"")

{-
OUTPUT:
"1"
"(3.0, [])"
"\"hi\""
1
(3.0, [])
"hi"
-}
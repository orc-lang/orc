  Vwait(0) >> Write(1)
| Vwait(1) >> Write((3.0, []))
| Vwait(2) >> Write("hi")
| Vwait(3) >> Read("1")
| Vwait(4) >> Read("(3.0, [])")
| Vwait(5) >> Read("\"hi\"")

{-
OUTPUT:
"1"
"(3.0, [])"
"\"hi\""
1
(3.0, [])
"hi"
-}
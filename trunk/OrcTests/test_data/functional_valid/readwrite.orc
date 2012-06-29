{- readwrite.orc -- Simple test for Orc Read and Write sites
 - 
 - $Id$
 - 
 - Created by quark
 -}

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Vawait(0) >> Write(1)
| Vawait(1) >> Write((3.0, []))
| Vawait(2) >> Write("hi")
| Vawait(3) >> Read("1")
| Vawait(4) >> Read("(3.0, [])")
| Vawait(5) >> Read("\"hi\"")
)

{-
OUTPUT:
"1"
"(3.0, [])"
"\"hi\""
1
(3.0, [])
"hi"
-}

{- readwrite.orc -- Simple test for Orc Read and Write sites
 - 
 - $Id$
 - 
 - Created by quark
 -}

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( Vawait(0) >> Write(1)          >v> Println(Write(v)) >> stop
| Vawait(1) >> Write((3.0, []))  >v> Println(Write(v)) >> stop
| Vawait(2) >> Write("hi")       >v> Println(Write(v)) >> stop
| Vawait(3) >> Read("1")         >v> Println(Write(v)) >> stop
| Vawait(4) >> Read("(3.0, [])") >v> Println(Write(v)) >> stop
| Vawait(5) >> Read("\"hi\"")    >v> Println(Write(v)) >> stop
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

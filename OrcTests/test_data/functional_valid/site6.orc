{- site1.orc -- Test a simple site call
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

site Test(x :: Integer) :: Integer = x + 1

Test(10) >x> Println(x) >> stop ; "Halted"

{-
OUTPUT:
11
"Halted"
-}

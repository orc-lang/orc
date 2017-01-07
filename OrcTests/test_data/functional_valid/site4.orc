{- site4.orc -- Test call halting if site declaration is terminated
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

val Passed = {| site Test(x :: Integer) :: Integer = x+1 | x # Test |}
Passed(10) ; "halted"

{-
OUTPUT:
"halted"
-}

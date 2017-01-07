{- site1.orc -- Test a simple site call
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

((site Test(x :: Integer) :: Integer = x + 1
Println(Test(10)) >> stop) ; "Detected!!")
|
Rwait(3000) >> Println("3 sec") >> stop

{-
OUTPUT:
11
3 sec
-}

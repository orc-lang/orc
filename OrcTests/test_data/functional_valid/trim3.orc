{- trim2.orc -- Test termination in trim
 -
 - Created by amp on Nov 28, 2014
 -}

{| 
Println(0) >> stop | 
Rwait(100) >> Println(1) >> stop | 
Rwait(200) >> Println(2) | 
Rwait(300) >> Println(3)
|} >> stop

{-
OUTPUT:
0
1
2
-}

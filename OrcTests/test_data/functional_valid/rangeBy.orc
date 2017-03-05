{- rangeBy.orc -- Orc program rangeBy
 -
 - Created by amp on Mar 5, 2017 3:56:27 PM
 -}

Println(rangeBy(1, 8, 2)) >>
Println(rangeBy(1, 8, 1.1)) >>
Println(last(rangeBy(0, 20000, 1001))) >>
Println(last(rangeBy(0, 20000, 1000))) >>
Println(last(rangeBy(3, 20001, 2))) >>
stop

{-
OUTPUT:
[1, 3, 5, 7]
[1, 2.1, 3.2, 4.3, 5.4, 6.5, 7.6]
19019
19000
19999
-}
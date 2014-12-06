{- site5.orc -- Test site call terminating when declaration is killed while it is running
 -
 - $Id$
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

val cell = Cell()

{| site Test(x) = x | Rwait(200) >> Println("Should not") # cell := Test >> stop | Rwait(100) |} >> stop |

(
val Passed = cell? #
(Passed(10) >x> Println(x) >> stop ; "halted")
|
Rwait(300) >> "later"
)

{-
OUTPUT:
10
"halted"
"later"
-}
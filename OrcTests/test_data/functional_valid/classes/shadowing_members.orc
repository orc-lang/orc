{- shadowing_members.orc -- Orc program shadowing_members
 -
 - Created by amp on Feb 21, 2015 5:02:11 PM
 -}

val o = new {
  val x = 0
  val y = 
    42 >x> x
}

o.y

{-
OUTPUT:
42
-}

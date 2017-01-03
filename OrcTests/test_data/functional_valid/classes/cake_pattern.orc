{- cake_pattern.orc -- Orc program cake_pattern
 -
 - Created by amp on Jan 30, 2015 6:54:54 PM
 -}

class Base {
  val x :: Integer
}

class C100 extends Base {
  val x = 100
}

class Plus10 extends Base {
  val x = super.x + 10
}

class Times10 extends Base {
  val x = super.x * 10
}

Println( (new C100 with Plus10 with Times10).x ) >>
Println( (new C100 with Times10 with Plus10).x ) >>
stop


{-
OUTPUT:
1100
1010
-}
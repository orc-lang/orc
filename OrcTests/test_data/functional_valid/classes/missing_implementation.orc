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

{|
Rwait(100) |
Println( (new Plus10 with Times10).x )
|} >>
stop

-- TODO: This should eventually be ruled out by the type checker. Then this test should be moved to functional_invalid.
{-
OUTPUT:
Error: orc.error.runtime.NoSuchMemberException: Value {  } does not have a 'x' member
-}
     
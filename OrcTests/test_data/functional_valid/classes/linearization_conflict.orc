{- linearization_conflict.orc -- Orc program linearization_conflict
 -
 - $Id$
 -
 - Created by amp on Feb 7, 2015 3:35:19 PM
 -}

class B1 {
  val y :: Integer = 1
}

class B2  {
  val y :: Integer = 2
}

class C1 extends B1 with B2 {}

class C2 extends B2 with B1 {}

class B3  {
  val x :: Integer = 2
}

class C3 extends B1 with B3 {}

class C4 extends B3 with B1 {}

(new (C1 with C2)
|
new (C3 with C4))
>> stop


-- TODO: Add support for testing for the presence of specific compiler erros and warning.
{-
Expected compile warnings:
Error: orc.error.runtime.ConflictingOrderWarning: Classes are in different orders in linearizations of mix-ins. B2, B1 is different from B1, B2 [[OrcWiki:ConflictingOrderWarning]]
(new (C1 with C2)
Error: orc.error.runtime.ConflictingOrderWarning: Classes are in different orders in linearizations of mix-ins. B3, B1 is different from B1, B3 [[OrcWiki:ConflictingOrderWarning]]
new (C3 with C4))
-}

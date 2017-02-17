{- constructors.orc -- Orc program constructors
 -
 - Created by amp on Mar 1, 2015 1:35:32 PM
 -}

class def C(a) {
  def f() = D(a)
}
class def D(a) {
  def f() = C(a)
}

C(2).f().f().f().a

{-
-- TODO: Reenable or remove based on decision on constructors.
-- OUTPUT:
2
-}

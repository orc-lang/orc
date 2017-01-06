{- constructors.orc -- Orc program constructors
 -
 - Created by amp on Mar 1, 2015 1:35:32 PM
 -}

class def C(a :: Integer) :: C {
  def f() = D(a)
}
class def D(a :: Integer) :: D {
  def f() = C(a)
}

C(2).f().f().f().a

{-
OUTPUT:
2
-}
{- constructors.orc -- Orc program constructors
 -
 - Created by amp on Mar 1, 2015 1:35:32 PM
 -}

class def C(a :: Integer):: C {
  def f() = a
}
{-
class C {
  val C :: lambda() :: C
  val a :: Integer
  def f() = a
}
def C(a_) = new C with { val C = C # val a = a_ }
-}

C(2).f()

{-
-- TODO: Reenable or remove based on decision on constructors.
-- OUTPUT:
2
-}

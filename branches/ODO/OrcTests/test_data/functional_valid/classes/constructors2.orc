{- constructors.orc -- Orc program constructors
 -
 - $Id$
 -
 - Created by amp on Mar 1, 2015 1:35:32 PM
 -}

class def C(a :: Integer) :: C {
  def f() = D(a)
}
class def D(a :: Integer) :: D {
  def f() = C(a)
}
{-
class C {
  val C :: lambda() :: C
  val D :: lambda() :: D
  val a :: Top
  def f() = D(a)
}
class D {
  val C :: lambda() :: C
  val D :: lambda() :: D
  val a :: Top
  def f() = C(a)
}
def C(a_) = new C with { val C = C # val D = D # val a = a_ }
def D(a_) = new D with { val C = C # val D = D # val a = a_ }
-}

C(2).f().f().f().a

{-
OUTPUT:
2
-}
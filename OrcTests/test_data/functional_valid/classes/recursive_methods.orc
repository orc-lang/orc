{- recursive_methods.orc -- Test that recursive methods bind properly and don't dead-lock.
 -
 - $Id$
 -
 - Created by amp on Feb 20, 2015 5:24:18 PM
 -} 

class B {
  def f(0 :: Integer) :: Integer = 0
  def f(n) = f(n - 1) + 1
  
  def g(0 :: Integer) :: Integer = 0
  def g(n) =
    def h() = g(n - 1) + 1
    h()
}

class C extends B {
  def f(0 :: Integer) :: Integer = 0
  def f(n) = super.f(n) + 2
  
  def g(0 :: Integer) :: Integer = 0
  def g(n) =
    def h() = super.g(n) + 2
    h()
}

(
val b = new B
Println(b.f(5))
) >>
(
val c = new C
Println(c.f(5))
) >>
(
val b = new B
Println(b.g(5))
) >>
(
val c = new C
Println(c.g(5))
) >>
stop

-- | Rwait(1000) >> DumpState()

{-
OUTPUT:
5
15
5
15
-}
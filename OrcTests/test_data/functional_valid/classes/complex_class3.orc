-- A test of an abstract class and instantiation

class B {
  def f(Integer) :: Integer
  val y :: Integer
}

class C extends B {
  val x :: Integer = y
  def f(i) = i + x
}

val o = new C { val y = 42 # val z = f(1) }
Println(o.x) >> Println(o.z) >> stop

{-
OUTPUT:
42
43
-}
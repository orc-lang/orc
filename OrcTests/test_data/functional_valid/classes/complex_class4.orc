-- A test of an abstract class and instantiation

class B {
  val f
  val y
}

class C extends B {
  val x = y
  def f(i) = i + x
}

val o = new C { val y = 42 # val z = f(1) }
Println(o.x) >> Println(o.z) >> stop

{-
OUTPUT:
42
43
-}
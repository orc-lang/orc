-- A test of a trivial class and instantiation

class B {
  val y :: Integer
}

class C extends B {
  val x :: Integer = y
}

(new C with { val y = 42 }).x

{-
OUTPUT:
42
-}
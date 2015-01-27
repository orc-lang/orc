-- A test of a trivial class and instantiation

class B {
  -- An abstract member
  --val y :: Integer
}

class C extends B {
  val x :: Integer = this.y
}

(new C with { val y = 42 }).x

{-
OUTPUT:
42
-}
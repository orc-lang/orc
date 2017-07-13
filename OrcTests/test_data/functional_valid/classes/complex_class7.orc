-- A test of a trivial class and instantiation with subclassing and captured variable

val captured = 3

class B {
  -- An abstract member
  --val y :: Integer
}

class C extends B {
  val x :: Integer = this.y + captured
}

(new C { val y = 42 }).x

{-
OUTPUT:
45
-}

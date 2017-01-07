-- A test of a trivial class and instantiation

class B {
  val y :: Integer = 10
}

class C extends B {
  val x :: Integer = y
}

val o = new (C with { val y = 42 # val z = 2 }) { val z' = z + x }
Println(o.x) >> Println(o.z') >> stop

{-
OUTPUT:
42
44
-}

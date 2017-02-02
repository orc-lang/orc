-- TODO: Remove this file.

class B1 {
  val y :: Integer
}

class B2 {
  val z :: Integer
}

class B3 {
  val z :: Integer
}

class C extends B1 with B2 with {
  val z = 8
} with B3 {
  val x :: Integer = y + z
}

(new C { val y = 42 }).x

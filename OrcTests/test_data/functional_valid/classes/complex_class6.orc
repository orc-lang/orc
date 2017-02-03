-- Test C3 linearization

class O { val order = ["O"] }
class A extends O { val order = "A" : super.order }
class B extends O { val order = "B" : super.order }
class C extends O { val order = "C" : super.order }
class D extends O { val order = "D" : super.order }
class E extends O { val order = "E" : super.order }
class K1 extends C with B with A { val order = "K1" : super.order }
class K2 extends E with B with D { val order = "K2" : super.order }
class K3 extends A with D { val order = "K3" : super.order }
class Z extends K3 with K2 with K1 { val order = "Z" : super.order }

(new Z).order 

{-
OUTPUT:
["Z", "K1", "K2", "K3", "D", "A", "B", "C", "E", "O"]
-}
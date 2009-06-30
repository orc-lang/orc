def adder1(Integer)(Integer) :: Integer
def adder1(x)(y) = x + y

val adder2 = lambda(x :: Integer)(y :: Integer) = x + y

  adder1(5) >a> a(2)
| adder2(5) >a> a(2)
| adder1(5)(2)

{-
OUTPUT:
7
7
7
-}
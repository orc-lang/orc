def adder1(Integer) :: lambda(Integer) :: Integer
def adder1(x) = lambda(y) = x + y

val adder2 = lambda(x :: Integer) = lambda(y :: Integer) = x + y

  adder1(5) >a> a(2)
| adder2(5) >a> a(2)
| adder1(5)(2)

{-
OUTPUT:
7
7
7
-}
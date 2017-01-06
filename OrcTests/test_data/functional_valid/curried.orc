def adder1(Integer) :: lambda(Integer) :: Integer
def adder1(x) = { x +  (_ :: Integer) }

val adder2 = def f(x :: Integer) = { x + (_ :: Integer) } # f

  adder1(5) >a> a(2)
| adder2(5) >a> a(2)
| adder1(5)(2)

{-
OUTPUT:
7
7
7
-}

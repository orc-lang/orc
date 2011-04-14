val r = Ref[Integer]()

r := 5 >>
Println("r=" + r?) >>
r := 6 >>
Println("r=" + r?) >>
stop
{-
OUTPUT:
r=5
r=6
-}
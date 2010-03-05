val r = Ref[Integer]()

r := 5 >>
println("r=" + r?) >>
r := 6 >>
println("r=" + r?) >>
stop
{-
OUTPUT:
r=5
r=6
-}
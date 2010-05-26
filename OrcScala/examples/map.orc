val m = Map[String,Integer]()
m("1") := 1 >>
m("2") := 2 >>
println(m("1")?) >>
println(m("2")?) >>
stop
{-
OUTPUT:
1
2
-}
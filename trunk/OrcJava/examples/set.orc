val s = Set[String]()
s("1") := true >>
s("2") := false >>
println(s("1")?) >>
println(s("2")?) >>
println(s("3")?) >>
stop

{-
OUTPUT:
true
false
false
-}
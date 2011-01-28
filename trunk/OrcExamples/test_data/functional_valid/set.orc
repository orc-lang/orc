val s = Set[String]()
s("1") := true >>
s("2") := false >>
Println(s("1")?) >>
Println(s("2")?) >>
Println(s("3")?) >>
stop

{-
OUTPUT:
true
false
false
-}
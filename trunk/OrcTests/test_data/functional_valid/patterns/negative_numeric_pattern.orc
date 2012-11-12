-- Test pattern matching on negative numeric literals

{-
OUTPUT:PERMUTABLE
"ISuccess"
"FSuccess"
-}

(-1 >-1> "ISuccess" ; "Fail") |
(-2.3 >-2.3> "FSuccess" ; "Fail") 
class Double = java.lang.Double
class Float = java.lang.Float
class Long = java.lang.Long
class Integer = java.lang.Integer
class Short = java.lang.Short

val x = Integer.valueOf(3)

(
-- test coercion
println(x.getClass()) >>
println(Integer.toString(x)) >>
stop
;
-- test widening
println(x.getClass()) >>
println(Long.toString(x)) >>
println(Double.toString(x)) >>
stop
;
-- test narrowing (error is expected)
println(x.getClass()) >>
println(Short.toString(x)) >>
stop

) 
:!: Bot  
{- 
  The Orc typechecker doesn't have enough info to check widening/narrowing conversions correctly.   
-}


{-
OUTPUT:
class java.lang.Integer
3
class java.lang.Integer
3
3.0
class java.lang.Integer
Error: java.lang.IllegalArgumentException: argument type mismatch
-}
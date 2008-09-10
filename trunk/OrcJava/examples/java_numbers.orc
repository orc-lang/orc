class Double = java.lang.Double
class Float = java.lang.Float
class Long = java.lang.Long
class Integer = java.lang.Integer
class Short = java.lang.Short

-- test coercion
3 >x> (
    println(x.getClass()) >> stop
  | println(Integer.toString(x)) >> stop
)
;
-- test widening
Integer.valueOf(3) >x> (
    println(x.getClass()) >> stop
  | println(Long.toString(x)) >> stop
  | println(Double.toString(x)) >> stop
)
;
-- test narrowing (error is expected)
Integer.valueOf(3) >x> (
    println(x.getClass()) >> stop
  | println(Short.toString(x)) >> stop
)
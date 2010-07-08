class Int = java.lang.Integer
class ConsValue = orc.runtime.values.ConsValue

println(Int.MIN_VALUE?) >> -- static field
println(Int.decode("5")) >> -- static method
stop

{-
OUTPUT:
-2147483648
5
-}
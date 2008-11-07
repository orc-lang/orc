class Integer = java.lang.Integer
class ConsValue = orc.runtime.values.ConsValue

println(Integer.MIN_VALUE?) >> -- static field
println(Integer.decode("5")) >> -- static method
ConsValue(1, []) >v>
println(v.head?) >> -- field read
v.head := (1,2) >> -- field write
println(v.head?) >> -- field read
println(v.enlist()) >> -- method
stop
{-
OUTPUT:
-2147483648
5
1
(1, 2)
[(1, 2)]
-}
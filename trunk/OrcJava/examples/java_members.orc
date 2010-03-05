class Int = java.lang.Integer
class ConsValue = orc.runtime.values.ConsValue

println(Int.MIN_VALUE?) >> -- static field
println(Int.decode("5")) >> -- static method
ConsValue[(Integer,Integer)]((0,1), [])  >v>
println(v.head?) >> -- field read
v.head := (1,2) >> -- field write
println(v.head?) >> -- field read
println(v.enlist()) >> -- method
stop

{-
OUTPUT:
-2147483648
5
(0, 1)
(1, 2)
[(1, 2)]
-}
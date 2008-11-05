class Integer = java.lang.Integer
class SomeValue = orc.runtime.values.SomeValue

println(Integer.MIN_VALUE?) >> -- static field
println(Integer.decode("5")) >> -- static method
SomeValue(5) >v>
println(v.content?) >> -- field read
v.content := 6 >> -- field write
println(v.untag()) >> -- method
stop
{-
OUTPUT:
-2147483648
5
5
6
-}
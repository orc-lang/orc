class Integer = java.lang.Integer
class TaggedValue = orc.runtime.values.TaggedValue

println(Integer.MIN_VALUE?) >> -- static field
println(Integer.decode("5")) >> -- static method
TaggedValue((5,6), null) >v>
v.payload := (1,2) >> -- field write
println(v.payload?) >> -- field read
println(v.signal()) >> -- method
stop
{-
OUTPUT:
-2147483648
5
(1, 2)
signal
-}
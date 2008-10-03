class Integer = java.lang.Integer
class SomeValue = orc.runtime.values.SomeValue

Integer.MIN_VALUE -- static field
| Integer.decode("5") -- static method
| SomeValue(5) >v> (
    v.content -- field
    | v.untag() -- method
  )
{-
OUTPUT:
-2147483648
5
5
5
-}
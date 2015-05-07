{-
This illustrates how rules for newlines resolve
ambiguous parses.
-}
def ambiguous1() =
  def ambiguous2() =
    4
    + 5
  -2
(3)
{-
OUTPUT:
3
-}
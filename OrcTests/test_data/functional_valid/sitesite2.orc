{-
  An Orc site reports halting.
-}

def silence() = Rwait(1000) >> stop
val Silence = MakeSite0(silence)

Let( (Silence() ; true) | Rwait(2000) >> false )

{-
OUTPUT:
true
-}

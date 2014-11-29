{-
  An Orc site reports halting.
-}

def silence() = Rwait(1000) >> stop
val Silence = MakeSite(silence)

{| (Silence() ; true) | Rwait(2000) >> false |}

{-
OUTPUT:
true
-}

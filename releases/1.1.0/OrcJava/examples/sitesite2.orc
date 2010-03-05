{-
  An Orc site reports halting.
-}

def silence() = Rtimer(1000) >> stop
val Silence = Site(silence)

let( (Silence() ; true) | Rtimer(2000) >> false )

{-
OUTPUT:
true
-}
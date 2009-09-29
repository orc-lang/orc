{-
  An Orc site reports halting.
-}

def silence() = Rtimer(1000) >> stop
val Silence = Site(silence)

let( (silence() ; true) | Rtimer(2000) >> false )

{-
OUTPUT:
true
-}
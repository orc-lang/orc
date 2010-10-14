{- Orca3.orc
 - 
 - Created by dkitchin on 14 Oct 2010
 -}

def pump(0,_) = stop
def pump(n,t) = Rtimer(random(t)) >> signal | pump(n-1,t)

val r = Ref(0)
def inc() = atomic ( r := r? + 1 >> r? )

pump(100, 1000) >> inc() >> stop ; r?

{-
OUTPUT:
100
-}
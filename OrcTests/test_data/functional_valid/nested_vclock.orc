{- nested_vclock.orc -- Orc test of nested virtual clocks
 - 
 - $Id$
 - 
 - Created by quark
 -}

def Vwait(t :: Integer) = Vawait(t + (Vtime() :!: Integer))

def simulate(n :: String) =
  val x = Rwait(100)
    stop
  | Rwait(0)  >> Vwait(3) >> n+":    y "+Vtime()  >v> Println(v) >> stop
  | Rwait(50) >> Vwait(2) >> n+":   x "+Vtime()   >v> Println(v) >> stop
  | Rwait(150) >> Vwait(1) >> n+":     z "+Vtime() >v> Println(v) >> stop
  -- nested simulation
  | Vclock(IntegerTimeOrder) >> Vawait(0) >>
      ( x >> Vwait(1) >> n+":  w "+Vtime() >v> Println(v) >> stop
      | Vwait(2)      >> n+": v "+Vtime()  >v> Println(v) >> stop
      )

Vclock(IntegerTimeOrder) >> Vawait(0) >>
( (Vclock(IntegerTimeOrder) >> Vawait(0) >> simulate("A"))
| Vwait(1) >> (Vclock(IntegerTimeOrder) >> Vawait(0) >> simulate("     B"))
) >> stop

{-
OUTPUT:
A: v 2
A:  w 3
A:   x 2
A:    y 3
A:     z 4
     B: v 2
     B:  w 3
     B:   x 2
     B:    y 3
     B:     z 4
-}

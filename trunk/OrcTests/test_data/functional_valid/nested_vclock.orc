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
  | Rwait(0) >> Vwait(3) >> n+": "+3
  | Rwait(100) >> Vwait(2) >> n+": "+2
  | Rwait(200) >> Vwait(1) >> n+": "+4
  -- nested simulation
  | Vclock(IntegerTimeOrder) >> Vawait(0) >> 
      ( x >> Vwait(1) >> n+": "+1
      | Vwait(2) >> n+": "+0
      )

  Vclock(IntegerTimeOrder) >> Vawait(0) >> 
( (Vclock(IntegerTimeOrder) >> Vawait(0) >> simulate("A"))
| Vwait(1) >> (Vclock(IntegerTimeOrder) >> Vawait(0) >> simulate("B"))
)

{-
OUTPUT:
"A: 0"
"A: 1"
"A: 2"
"A: 3"
"A: 4"
"B: 0"
"B: 1"
"B: 2"
"B: 3"
"B: 4"
-}

def simulate(n) =
  val x = Rtimer(100)
    stop
  | Rtimer(0) >> Ltimer(3) >> n+": "+4
  | Rtimer(100) >> Ltimer(2) >> n+": "+3
  | Rtimer(200) >> Ltimer(1) >> n+": "+2
  -- nested simulation
  | withLtimer(lambda () =
        x >> Ltimer(1) >> n+": "+0
        | Ltimer(2) >> n+": "+1)

  withLtimer(defer(simulate, "A"))
| Rtimer(500) >> withLtimer(defer(simulate, "B"))

{-
OUTPUT:
A: 0
A: 1
A: 2
A: 3
A: 4
B: 0
B: 1
B: 2
B: 3
B: 4
-}
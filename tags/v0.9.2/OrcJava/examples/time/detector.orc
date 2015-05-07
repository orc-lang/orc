{- Heisenbug detector. Make sure your processor fan is working. -}

def track(t, s) =
  def report(n) = Rtimer(t) >> print(s, ": ", n, "\n")
  def tick(n) = report(n) >> tick((n % 99) + 1)
  tick(1)

  
  track(2, "two") 
| track(3, "three") 
| track(5, "five") 
| track(7, "seven")
  
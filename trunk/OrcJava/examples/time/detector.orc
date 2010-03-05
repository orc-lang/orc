{- Heisenbug detector. Make sure your processor fan is working. -}

def track(Number, String) :: Bot
def track(t, s) =
  def report(Number) :: Signal
  def tick(Number) :: Bot
  def report(n) = Rtimer(t) >> println(s + ": " + n)
  def tick(n) = report(n) >> tick((n % 99) + 1)
  tick(1)

  
  track(2, "two") 
| track(3, "three") 
| track(5, "five") 
| track(7, "seven")
  
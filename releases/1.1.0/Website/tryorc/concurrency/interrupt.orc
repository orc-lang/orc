{-
Given a function f, return a pair of functions (run, kill):
"run" runs f subject to interruption, and "kill" interrupts
a running f.  Publications from f are suppressed.
-}
def try(f) =
  val c = Buffer()
  def kill() = c.put(signal)
  def run() = let( f() >> stop | c.get() ) >> stop
  (kill, run)

val (kill, run) =
  try( lambda () = metronome(1000) >> println("tick") )

run() | Prompt("Interrupt?") >> kill()

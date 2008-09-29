def try(f) =
  val c = Buffer()
  def kill() = c.put(signal)
  def run() = let( f() >> stop | c.get() ) >> stop
  (kill, run)

val (kill, run) =
  try( lambda () = Metronome() >> println("tick") )

run() | Prompt("Interrupt?") >> kill()

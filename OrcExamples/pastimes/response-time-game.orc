{- response-time-game.orc
 -
 - Created by misra on Jul 9, 2010 10:33:45 AM
 -}

include "../clock_time/Stopwatch.inc"
val sw = Stopwatch()

val (id,dd) = (3000,100)
-- id is the initial delay in starting a game
-- dd is the delay in printing digits

def rand_seq() :: Integer = Random(10) |  Rwait(dd) >> rand_seq()

def game() =
  {- game() conducts one game and returns a pair (b,w).
     b is true iff user responds only after v is printed;
     then the response time is w
  -}

  val v = Random(10) -- v is a random digit for one game

  val (b,w) =
      Rwait(id) >> rand_seq() >x> Println(x) >>
      Ift(x = v) >> sw.start() >> stop
    | Prompt("Press ENTER for SEED "+v) >>
      sw.isrunning() >b> sw.pause() >w> (b,w)

  {- Goal Expression of Game -}
  if b then
    ("Response time = " +w)
  else ("You jumped the gun.")

{- Goal Expression of the program -}
Println("This game measures your response time.") >>
Println("You will be shown a decimal digit, the SEED, for a few seconds.") >>
Println("Then you will see a stream of digits.") >>
Println("Press ENTER as soon as you see the SEED in this stream.") >>
Println("Your response time is the time you took to press ENTER after the SEED was first published.") >>
Prompt("Ready to go? Press ENTER to start the game") >>
game()

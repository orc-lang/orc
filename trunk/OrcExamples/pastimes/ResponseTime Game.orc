{- ResponseTime Game.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 16, 2010 6:31:51 PM
 -}
include "../clock_time/StopWatch.inc"

val (id,dd) = (3000,100)
-- id is the initial delay in starting a game
-- dd is the delay in printing digits

val sw = Stopwatch()

def rand_seq() = Random(10) |  Rwait(dd) >> rand_seq()

def game() =
{- game() conducts one game and returns a pair (b,w).
   b is true iff user responds only after v is printed; 
   then the response time is w 
-}
   val v = Random(10) -- v is a random digit for one game

   val (b,w) = 
 
       sw.reset() >> Rwait(id) >> rand_seq() >x> Println(x) >>
       Ift(x = v) >> sw.start() >> stop

     | Prompt("Press ENTER/OK for "+v) >> 
       sw.isrunning() >b> sw.halt() >w> (b,w)
   
   signal >> (b,w)

def games() =
{- games() conducts multiple games, a session. 
   For each properly concluded game, it displays the response time.
   For a game that is prematurely concluded, it terminates the entire session.
   The only way to terminate a session is by hitting ENTER asap, or by
   terminating the run from Eclipse.
-}
  game() >(b,w)> 
  if b then
   (Println("Response time = " +w) >> games())
   else (Println("Game Over") >> stop)

games()

{-
Notes:
rand_seq() resembles a Metronome.

Definition of game() ensures that any user input causes termination of
  rand_seq() since it is a Orc function

If rand_seq() generates multiple values equal to v, sw.start() will be called
for each of them. But, beyond the first call, sw.start() has no effect.
-}

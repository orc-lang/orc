{- test.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 17, 2010 4:29:41 PM
 -}

include "http://www.cs.utexas.edu/users/misra/temporaryFiles.dir/StopWatch.inc"

val (id,dd) = (3000,100)
-- id is the initial delay in starting a game
-- dd is the delay in printing digits
val sw = Stopwatch()

def game() =
    val v = random(10) 
    def rand_seq() = random(10) >x> println(x) >> x |  Rtimer(dd) >> rand_seq()
  
val valid = 
    sw.reset() >> Rtimer(id) >> rand_seq() >x>
     If(x = v) >> sw.start() >> stop
  |  Prompt("Press ENTER for "+v) >> sw.isrunning() 


  if valid
   then (sw.halt() >w> println("Response time = " +w) >> game())
   else println("Game Over") >> stop

game()


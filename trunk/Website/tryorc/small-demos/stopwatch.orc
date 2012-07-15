{- stopwatch.orc -- Orc program which simulates a stopwatch
 -
 - Created by misra on Mar 15, 2010 4:10:00 PM
 -}

{- First, define a class for the stopwatch:

   supports four methods:
    start, pause and reset, isrunning

Only the operations causing a state change are shown below. Other
operations do not cause a state change.

   In initial state, start transits to running.
   In running state, either
     pause can be executed, transits to paused, or
     reset can be executed, transits to initial.
   In paused state, either
     start can be executed, transits to running, or
     reset can be executed, transits to initial.

   Schematically,

                           start
                    |<--------------------|
            start   |     pause           |
--->initial ----> running ----> paused ---+
      |                |                  |
      |<---------------+-------------------
                 reset


The state diagram can be minimized to two states: running, paused. State paused 
comprises the previous initial and paused. The first state is paused.
The transitions are:
 reset/pause always leaves the state in paused.
 start always leaves the state in running.

A call to reset returns a signal.
A call to start returns a signal.
A call to pause returns the current value on the stopwatch.
A call to isrunning returns a boolean, true iff it is running.

The state is encoded by a boolean, running.
lastpause is the time showing on the face of the stopwatch when the
last call to pause was made. Initially, lastpause = 0 and also
after each call to reset.

laststart is the time at which the last call to start was made.
-}

def class Stopwatch() =

  val (laststart, lastpause) = (Ref(0), Ref(0))
  val running = Ref(false)
  val sem = Semaphore(1)

  def start() = 
    sem.acquire() >>  
    ( if running? then signal
      else (running := true >> laststart := Rtime()) 
    ) >>
    sem.release()

  def pause() =
    sem.acquire() >>  
    ( if running? then
        lastpause := lastpause? + Rtime() - laststart? >>
        running := false 
      else signal
    ) >>
    sem.release() >> lastpause?

  def reset() =
    sem.acquire() >>  
    running := false >> lastpause := 0 >>
    sem.release()

  def isrunning() = running? -- single atomic step

  stop

{- Test
   The output is the time it takes to do a single search. Currently, only
   the Google search engine is supported. Replace the arguments in the
   call to test to search for a different string or to compute the running
   time of other sites, such as test(Rwait,100).
-}

include "net.inc"
val Google = GoogleSearchFactory("orc/orchard/orchard.properties")

val sw = Stopwatch()
def test(s,v) =
  sw.reset() >>
  sw.start() >> s(v) >> sw.halt()

test(Google,"Edsger W. Dijkstra") >t>
("time taken = " + t)

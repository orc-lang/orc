{- Stopwatch.inc -- Orc include file stopwatch.inc
 -
 - Created by misra on Mar 15, 2010 4:10:00 PM
 -}


{- supports four methods:
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

  val (laststart, lastpause) = (Ref[Integer](0), Ref[Integer](0))
  val running = Ref[Boolean](false)
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

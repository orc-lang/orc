{- stopwatch.inc -- Orc include file stopwatch.inc
 -
 - $Id$
 -
 - Created by misra on Mar 15, 2010 4:10:00 PM
 -}

{- supports four methods:
    start, stop and reset, isrunning

Only the operations causing a state change are shown below. Other
operations do not cause a state change.

   In initial state, start transits to running.
   In running state, reset/stop transits to stopped.
   In stopped state, either
     start can be executed, transits to running, or
     reset can be executed, transits to initial.

   Schematically,

                           start
                    |<--------------------|
            start   |      stop           |
--->initial ----> running ----> stopped --|
      |              |                    |
      |<-----------------------------------
                       reset


The state diagram can be minimized to two states: running, stopped. State stopped
comprises the previous initial and stopped. The first state is stopped.
The transitions are:
 reset/stop always leaves the state in stopped.
 start always leaves the state in running.

A call to reset returns a signal.
A call to start returns a signal.
A call to stop returns the current value on the stopwatch.
A call to isrunning returns a boolean, true iff it is running.

The state is encoded by a boolean, running.
timeshown is the time showing on the face of the stopwatch when the
last call to stop was made. Initially, timeshown = 0 and also
after each call to reset.

laststart is the time at which the last call to start was made.
-}

def class Stopwatch() =
  val clk        = Rclock()
  val timeshown  = Ref(0)
  val laststart  = Ref(0)
  val running    = Ref(false)

  def reset() =
    running := false >> timeshown := 0

  def start() = -- record the clock value just before the end of this routine
    if (running?)
      then signal
      else (running := true >> laststart := clk.time() )

  def halt() = -- record the clock value as soon as this routine is entered
    clk.time() >c>
    (if (running?)
     then (running := false >>
           timeshown? + c - laststart? >v>  timeshown := v  >>
           v)
     else timeshown?)

  def isrunning() = running?

  stop

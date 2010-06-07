{- StopWatch.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 15, 2010 4:10:00 PM
 -}

{- supports four methods:
    start, stop and reset, isrunning

   In initial state, only start can be executed. Transits to running.
   In running state, only stop can be executed. Transits to stopped.
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


A call to start or reset returns a signal. 
A call to stop returns the current value on the stopwatch.
A call to isrunning returns a boolean, true iff it is running.
Any call in an inappropriate state is ignored (as skip).
reset is applicable in any state.

States are coded as, 0: initial, 1: running, 2: stopped
cumstop is the cumulative stoppage time.
laststop is the clk value when stop was executed last.
current stopwatch value is laststop? - cumstop?
-}

def capsule Stopwatch() =
val clk      = Clock()
val cumstop  = Ref(0)
val laststop = Ref(0)
val state    = Ref(0)
 
def reset() =  
 state := 0 >> cumstop := clk() >> laststop := clk()

def start() = 
 if (state? = 0 || state? = 2)
  then (state := 1 >> cumstop := cumstop? + clk() - laststop?)
  else signal

def stop() = 
 if (state? = 1)
  then (state := 2 >> laststop := clk() >> laststop? - cumstop?)
  else signal 

  def isrunning() = state?=1

stop

3


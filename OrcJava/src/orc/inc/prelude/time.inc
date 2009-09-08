{--
Real and logical time.
--}

{--
@site Rtimer(Integer) :: Top
Publish a signal after the given number of milliseconds.

@method Rtimer.time() :: Integer
Return the current real time in milliseconds, as
measured from midnight January 1, 1970 UTC.
Ranges from 0 to <code>Long.MAX_VALUE</code>.
--}
site Rtimer = orc.lib.time.Rtimer

{--
@def Clock()() :: Number
A call to <code>Clock</code> creates a new relative real-time clock.
Calling a relative clock returns the number
of milliseconds which have elapsed since the
clock was created.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes a value near 1000
val c = Clock()
Rtimer(1000) >> c()]]></programlisting>
--}
def Clock()() :: Integer
def Clock() =
  val time = Rtimer.time()
  lambda () = Rtimer.time() - time

{--
@site Ltimer(Integer) :: Top
Publish a signal after the given number of logical timesteps,
as measured by the current logical clock.
The logical time advances whenever the computation controlled
by the logical clock is quiescent (i.e. cannot advance on its own).

@method Ltimer.time() :: Integer
Return the current logical time, as measured by logical
clock which was current when <code>Ltimer.time</code> was evaluated.
Ranges from 0 to <code>Integer.MAX_VALUE</code>.
--}
site Ltimer = orc.lib.time.Ltimer

{--
@def withLtimer[A](lambda () :: A) :: A
Run the given thunk in the context of a new inner logical clock.
Within the computation represented by the thunk, calls to
<code>Ltimer</code> refer to the new clock. The
outer clock can only advance when the inner clock becomes
quiescent.
--}
def withLtimer[A](lambda () :: A) :: A
def withLtimer(thunk) =
  site pushLtimer = orc.lib.time.PushLtimer
  site popLtimer = orc.lib.time.PopLtimer
  thunk >> -- wait for free variables to become bound
  pushLtimer() >>
  thunk() >x>
  popLtimer() >>
  x

{--
@def metronome(Integer) :: Top
Publish a signal at regular intervals, indefinitely. The period is given by the
argument, in milliseconds.
--}
def metronome(Integer) :: Top
def metronome(t) = signal | Rtimer(t) >> metronome(t)

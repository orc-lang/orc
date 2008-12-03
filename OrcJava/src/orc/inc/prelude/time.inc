{--
Real and logical time.
--}

{--
* site Rtimer(Integer) :: Signal
Publish a signal after the given number of milliseconds.
--}
site Rtimer = orc.lib.time.Rtimer

{--
* site Clock()() :: Integer
A call to <code>Clock</code> creates a new relative clock.
Calling a relative clock returns the number
of milliseconds which have elapsed since the
clock was created.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes a value near 1000
val c = Clock()
Rtimer(1000) >> c()]]></programlisting>
--}
site Clock = orc.lib.time.Clock 

{--
* site Ltimer(Integer) :: Signal
Publish a signal after the given number of logical timesteps.
A logical timestep is complete as soon as all outstanding site
calls (other than calls to <code>Ltimer</code>) have published.
--}
site Ltimer = orc.lib.time.Ltimer

{--
* def withLtimer(lambda () :: A) :: A
Run a thunk in the context of a new logical clock.
--}
def withLtimer(thunk) =
  site pushLtimer = orc.lib.time.PushLtimer
  site popLtimer = orc.lib.time.PopLtimer
  thunk >> -- wait for free variables to become bound
  pushLtimer() >>
  thunk() >x>
  popLtimer() >>
  x

{--
* def metronome(Integer) :: Signal
Publish a signal at regular intervals, indefinitely. The period is given by the
argument, in milliseconds.
--}
def metronome(t) = signal | Rtimer(t) >> metronome(t)

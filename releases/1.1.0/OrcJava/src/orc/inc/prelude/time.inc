--
-- time.inc -- Orc standard prelude include, time section
-- Project OrcJava
--
-- $Id$
--
-- Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{--
Real and logical time.
--}

{--
@site Rtimer(Integer) :: Signal
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
@site Ltimer(Integer) :: Signal
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
@def metronome(Integer) :: Signal
Publish a signal at regular intervals, indefinitely. The period is given by the
argument, in milliseconds.
--}
def metronome(Integer) :: Signal
def metronome(t) = signal | Rtimer(t) >> metronome(t)


{-
@def timeout[A](A, Integer) :: Option[A]
Wait for a variable to become bound, up to the specified timeout in milliseconds.
If it does become bound to a value v, publish Some(v).
If instead the timeout is reached, publish None().
-}
def timeout[A](A, Integer) :: Option[A]
def timeout(x, t) = let(Some(x) | Rtimer(t) >> None())


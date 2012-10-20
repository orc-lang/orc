--
-- time.inc -- Orc standard prelude include, time section
-- Project OrcScala
--
-- $Id$
--
-- Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{--
Real time.
--}

{--
@site Rclock() :: {. time :: (lambda () :: Integer), wait :: (lambda (Integer) :: Signal) .}

Create a new realtime clock instance.


@method rclock.wait(Integer) :: Signal

Return a <link linkend="ref.data.signal">signal</link> after the given number of milliseconds.


@method rclock.time() :: Integer

Return the total number of milliseconds that have passed
since this clock was created.
Ranges from 0 to <code language="java">Long.MAX_VALUE</code>.

--}
import site Rclock = "orc.lib.time.Rclock"

{--
@site Rwait(Integer) :: Signal

Return a <link linkend="ref.data.signal">signal</link> after the given number of milliseconds.

@site Rtime() :: Integer

Return the total number of milliseconds that have passed
since this program began executing.
Ranges from 0 to <code language="java">Long.MAX_VALUE</code>.
--}
val (Rwait, Rtime) = (r.wait, r.time) <r< Rclock()

{--
@def metronome(Integer) :: Signal
Publish a <link linkend="ref.data.signal">signal</link> at regular intervals, indefinitely.
The period is given by the argument, in milliseconds.
--}
def metronome(Integer) :: Signal
def metronome(t) = signal | Rwait(t) >> metronome(t)

{-
@def timeout[A](A, Integer) :: Option[A]
Wait for a variable to become bound, up to the specified timeout in milliseconds.
If it does become bound to a value v, publish Some(v).
If instead the timeout is reached, publish None().
-}
def timeout[A](A, Integer) :: Option[A]
def timeout(x, t) = Let(Some(x) | Rwait(t) >> None())


{-
Virtual time.
-}

{--
@site Vclock(TotalSite) :: Signal

Create a new virtual clock instance, using the supplied site as a time value comparator.

The time value comparator site's type should be <code>(A, A) :: Integer</code>, where <code>A</code> is the
type of time values.  The returned integer should be <code>-1</code>, <code>0</code>, or <code>1</code>, depending on
whether argument one is less then, equal to, or greater than argument two, respectively.
--}
import site Vclock = "orc.lib.time.Vclock"

{--
@site Vawait[A](A) :: Boolean

Wait until the current virtual time is equal to the given value.
If the current time is presently the given value, return <code>false</code> immediately.
If the given value is in the future, wait for it, and return <code>true</code>.
If the given value is in the past, halt silently.
If no virtual time has been established, initialize to the given value and return <code>true</code>.

The type <code>A</code> of the parameter is determined by the type accepted by the time value
comparator supplied to the <code>Vclock</code> call that created the current virtual clock.
--}
import site Vawait = "orc.lib.time.Vawait"

{--
@site Vtime[A]() :: A

Return the current virtual time.
If none has been established, then halt silently.

The type <code>A</code> of the return value is determined by the type accepted by the time value
comparator supplied to the <code>Vclock</code> call that created the current virtual clock.
--}
import site Vtime = "orc.lib.time.Vtime"

{--
@site IntegerTimeOrder(Integer, Integer) :: Integer

A time value comparator for integer time values.
Pass this site to <code>Vclock</code> to create an integer virtual time scale.
--}
import site IntegerTimeOrder = "orc.lib.time.IntegerTimeOrder"

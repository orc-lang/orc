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

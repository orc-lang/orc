--
-- time.inc -- Orc standard prelude include, time section
-- Project OrcScala
--
-- $Id$
--
-- Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{--
Real and virtual time.
--}

{--
@site Rwait(Integer) :: Signal

<sitepropset>
   <siteprop propname="indefinite"/>
</sitepropset>

Publish a signal after the given number of milliseconds.

@method Rwait.time() :: Integer

<sitepropset>
   <siteprop propname="definite"/>
</sitepropset>

Return the current real time in milliseconds, as
measured from midnight January 1, 1970 UTC.
Ranges from 0 to <code>Long.MAX_VALUE</code>.
--}
site Rwait = "orc.lib.time.Rwait"

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
Rwait(1000) >> c()]]></programlisting>
--}
def Clock()() :: Number
def Clock() =
  val time = Rwait.time()
  lambda () = Rwait.time() - time

{--
@site Vwait(Integer) :: Signal

<sitepropset>
   <siteprop propname="indefinite"/>
</sitepropset>

Publish a signal after the given amount of virtual time.

@site Vclock() :: Integer

<sitepropset>
   <siteprop propname="definite"/>
</sitepropset>

Return the current virtual time.
--}
-- site Vwait = "orc.lib.time.Vwait"
def Vwait(Integer) :: Bot
def Vwait(_) = Error("Vwait not yet implemented")

-- site Vclock = "orc.lib.time.Vclock"
def Vclock() :: Bot
def Vclock() = Error("Vclock not yet implemented")

{--
@def metronome(Integer) :: Signal
Publish a signal at regular intervals, indefinitely. The period is given by the
argument, in milliseconds.
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

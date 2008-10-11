{--
site random() :: Integer
Return a random Integer value
chosen from the range of all possible 32-bit Integer values.

site random(Integer) :: Integer
Return a pseudorandom, uniformly distributed Integer value
between 0 (inclusive) and the specified value (exclusive).
If the argument is 0, fail silently.
--}
site random = orc.lib.util.Random

{--
site urandom() :: Double
Returns a pseudorandom, uniformly distributed Double value
between 0 and 1, inclusive.
--}
site urandom = orc.lib.util.URandom

{--
site UUID() :: String
Return a random (type 4) UUID represented as a string.
--}
site UUID = orc.lib.util.UUID

{--
site Thread(Site) :: Site
Given a site, return a new site which calls the original site
in a separate thread.  This is necessary when calling
a Java site which does not cooperate with Orc's scheduler
and may block for an unpredictable amount of time.

A limited number of threads are reserved in a pool for use
by this site, so there is a limit to the number of blocking,
uncooperative sites that can be called simultaneously.
--}
site Thread = orc.lib.util.ThreadSite

{--
def signals(Integer) :: Signal
Publish the given number of signals, simultaneously.
--}
def signals(n) = if n > 0 then (signal | signals(n-1))

{--
def for(Integer, Integer) :: Integer
Publish all values in the given half-open range, simultaneously.
--}
def for(low, high) =
  if low >= high then stop
  else ( low | for(low+1, high) )

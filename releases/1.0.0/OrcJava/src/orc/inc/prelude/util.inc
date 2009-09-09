{--
Miscellaneous utility functions.
--}

{--
@site random() :: Integer
Return a random Integer value
chosen from the range of all possible 32-bit Integer values.

@site random(Integer) :: Integer
Return a pseudorandom, uniformly distributed Integer value
between 0 (inclusive) and the specified value (exclusive).
If the argument is 0, halt.
--}
site random = orc.lib.util.Random

{--
@site urandom() :: Number
Returns a pseudorandom, uniformly distributed Double value
between 0 and 1, inclusive.
--}
site urandom = orc.lib.util.URandom

{--
@site UUID() :: String
Return a random (type 4) UUID represented as a string.
--}
site UUID = orc.lib.util.UUID

{--
@site Thread(Top) :: Bot
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
@site Prompt(String) :: String
Prompt the user for some input. The user may cancel the prompt,
in which case the site fails silently. Otherwise their response
is returned as soon as it is received.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes the user's name
Prompt("What is your name?")]]></programlisting>

The user response is always taken to be a string. Thus, integer 
3 as a response will be treated as "3". To convert the response 
to its appropriate data type, use the library function
<code>read</code>:

<programlisting language="orc-demo"><![CDATA[
-- Prompts the user to enter an integer, then parses the response.
Prompt("Enter an integer:") >r> read(r)]]></programlisting>
--}
site Prompt = orc.lib.util.Prompt

{--
@def signals(Integer) :: Top
Publish the given number of signals, simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes five signals
signals(5)]]></programlisting>

@implementation
--}
def signals(Integer) :: Top
def signals(n) = if n > 0 then (signal | signals(n-1))

{--
@def for(Integer, Integer) :: Integer
Publish all values in the given half-open range, simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 1 2 3 4 5
for(1,6)]]></programlisting>

@implementation
--}
def for(Integer, Integer) :: Integer
def for(low, high) =
  if low >= high then stop
  else ( low | for(low+1, high) )

{--
@def upto(Integer) :: Integer
<code>upto(n)</code> publishes all values in the range <code>(0..n-1)</code>
simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 1 2 3 4
upto(5)]]></programlisting>

@implementation
--}
def upto(Integer) :: Integer
def upto(high) = for(0, high)

{--
@def fillArray[A](Array[A], lambda (Integer) :: A) :: Array[A]
Given an array and a function from indices to values, populate the array
by calling the function for each index in the array.

For example, to set all elements of an array to zero:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 0 0
val a = fillArray(Array(3), lambda (_) = 0)
a.get(0) | a.get(1) | a.get(2)]]></programlisting>

@implementation
--}
def fillArray[A](Array[A], lambda (Integer) :: A)
  :: Array[A]
def fillArray(a, f) =
  val n = a.length()
  def fill(Integer, lambda(Integer) :: A) :: Bot
  def fill(i, f) =
    if i = n then stop
    else ( a.set(i, f(i)) >> stop
         | fill(i+1, f) )
  fill(0, f) ; a

{--
@def takePubs[A](Integer, lambda () :: A) :: A
<code>takePubs(n, f)</code> calls <code>f()</code>,
publishes the first <code>n</code> values published
by <code>f()</code> (as they are published), and then
halts.

@implementation
--}
def takePubs[A](Integer, lambda () :: A) :: A
def takePubs(n, f) =
  val out = Buffer[A]()
  val c = Counter(n)
  let(
    f() >x>
    if(c.dec() >> out.put(x) >> false
       ; out.closenb() >> true)
  ) >> stop | repeat(out.get)
  
{--
@def withLock[A](Semaphore, lambda () :: A) :: A
Acquire the semaphore and run a thunk which is expected to publish
no more than one value. Publishes the value published by the
thunk and releases the semaphore.

@implementation
--}
def withLock[A](Semaphore, lambda () :: A) :: A
def withLock(s, f) =
  s.acquire() >> (
    let(f()) >x>
    s.release() >>
    x
    ; s.release() >> stop
  )
 
{--
@def synchronized[A](Semaphore, lambda () :: A)() :: A
Given a lock and thunk, return a new thunk which is serialized
on the lock. Similar to Java's synchronized keyword.

@implementation
--}
def synchronized[A](Semaphore, lambda () :: A)() :: A
def synchronized(s,f)() = withLock(s, f)

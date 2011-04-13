--
-- state.inc -- Orc standard prelude include, state section
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
General-purpose supplemental data structures.
--}

-- Make the List type constructor available
import type List = "orc.lib.builtin.structured.ListType"

{--
@site Some[A](A) :: Option[A]
An optional value which is available.
This site may also be used in a pattern.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: (3, 4)
Some((3,4)) >s> (
    s >Some((x,y))> (x,y)
  | s >None()> signal
)]]></programlisting>

@site None[A]() :: Option[A]
An optional value which is not available.
This site may also be used in a pattern.

--}
import type Option = "orc.lib.builtin.structured.OptionType"
import site Some = "orc.lib.builtin.structured.SomeSite"
import site None = "orc.lib.builtin.structured.NoneSite"


{--
@site Cell[A]() :: Cell[A]
Create a write-once storage location.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 5 5
val c = Cell()
  c.write(5) >> c.read()
| Rwait(1) >> ( c.write(10) ; c.read() )]]></programlisting>

@method cell[A].read() :: A

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Read a value from the cell. If the cell does not yet have a value, <link linkend="ref.concepts.states.block">block</link> until it receives one.

@method cell[A].readD() :: A

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Read a value from the cell. If the cell does not yet have a value, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method cell[A].write(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Write a value to the cell. If the cell already has a value, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.
--}
import site Cell = "orc.lib.state.Cell"
import type Cell = "orc.lib.state.types.CellType"


{--
@site Ref[A]() :: Ref[A]
Create a rewritable storage location without an initial value.

Example:
<programlisting language="orc-demo"><![CDATA[
val r = Ref()
Rwait(1000) >> r := 5 >> stop
| Println(r?) >>
  r := 10 >>
  Println(r?) >>
  stop]]></programlisting>

@site Ref[A](A) :: Ref[A]
Create a rewritable storage location initialized to the provided value.

@method ref[A].read() :: A
Read the value of the ref. If the ref does not yet have a value, <link linkend="ref.concepts.states.block">block</link> until it receives one.

@method ref[A].readD() :: A
Read the value of the ref. If the ref does not yet have a value, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method ref[A].write(A) :: Signal
Write a value to the ref, then publish a <link linkend="ref.data.signal"><code>signal</code></link>.

--}
import site Ref = "orc.lib.state.Ref"
import type Ref = "orc.lib.state.types.RefType"

{--
@def (?)[A](Ref[A]) :: A
Get the value held by a reference.
<code>x?</code> is equivalent to <code>x.read()</code>.

@implementation
--}
def (?)[A](Ref[A]) :: A
def (?)(r) = r.read()

{--
@def (:=)[A](Ref[A], A) :: Signal
Set the value held by a reference.
<code>x := y</code> is equivalent to <code>x.write(y)</code>.

@implementation
--}
def (:=)[A](Ref[A], A) :: Signal
def (:=)(r,v) = r.write(v)

{--
@def swap[A](Ref[A], Ref[A]) :: Signal
Swap the values in two references, then return a signal.

@implementation
--}
def swap[A](Ref[A], Ref[A]) :: Signal
def swap(r,s) = (r?,s?) >(rval,sval)> (r := sval, s := rval) >> signal


{--
@site Semaphore(Integer) :: Semaphore
Return a semaphore with the given value. The semaphore maintains the invariant
that its value is always non-negative.

An example using a semaphore as a lock for a critical section:
<programlisting language="orc-demo"><![CDATA[
-- Prints:
-- Entering critical section
-- Leaving critical section
val lock = Semaphore(1)
lock.acquire() >>
Println("Entering critical section") >>
Println("Leaving critical section") >>
lock.release()]]></programlisting>

@method semaphore.acquire() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

If the semaphore's value is greater than <code>0</code>, decrement the semaphore and return a signal.
If the semaphore's value is <code>0</code>, <link linkend="ref.concepts.states.block">block</link> until it becomes greater than <code>0</code>.

@method semaphore.acquireD() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If the semaphore's value is greater than <code>0</code>, decrement the semaphore and return a signal.
If the semaphore's value is <code>0</code>, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method semaphore.release() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>


If any calls to <code>acquire</code> are <link linkend="ref.concepts.states.block">blocked</link>, allow the oldest such call
to return.  Otherwise, increment the value of the semaphore.  This may
increment the value beyond that with which the semaphore was constructed.  

@method semaphore.snoop() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

If any calls to <code>acquire</code> are <link linkend="ref.concepts.states.block">blocked</link>, return a signal.
Otherwise, <link linkend="ref.concepts.states.block">block</link> until some call to <code>acquire</code> <link linkend="ref.concepts.states.block">blocks</link>.

@method semaphore.snoopD() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If any calls to <code>acquire</code> are <link linkend="ref.concepts.states.block">blocked</link>, return a signal.
Otherwise, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.
--}
import site Semaphore = "orc.lib.state.Semaphore"
import type Semaphore = "orc.lib.state.types.SemaphoreType"

{--
@site Channel[A]() :: Channel[A]
Create a new asyncrhonous FIFO channel of unlimited size. A channel supports
get, put and close operations.

A channel may be either empty or non-empty, and either open or closed.  When
empty and open, calls to <code>get</code> <link linkend="ref.concepts.states.block">block</link>.  When empty and closed, calls
to <code>get</code> <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.  When closed, calls to <code>put</code> <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.  In all
other cases, calls return normally.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 10
val b = Channel()
  Rwait(1000) >> b.put(10) >> stop
| b.get()]]></programlisting>

@method channel[A].get() :: A

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Get an item from the channel. If the channel is open and no items are available,
<link linkend="ref.concepts.states.block">block</link> until one becomes available. If the channel is <link
linkend="ref.stdlib.state.Channel.close">closed</link> and no items are available, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method channel[A].getD() :: A

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Get an item from the channel. If no items are available, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method channel[A].put(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Put an item in the channel. If the channel is <link
linkend="ref.stdlib.state.Channel.close">closed</link>, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method channel[A].close() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

<firstterm xml:id="ref.stdlib.state.Channel.close">Close</firstterm> the channel and <link linkend="ref.concepts.states.block">block</link> until it is empty.
This has the effect of immediately causing any <link linkend="ref.concepts.states.block">blocked</link> calls to
<code>get</code> to <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. In addition, any subsequent calls to <code>put</code>
will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>, and once the channel becomes empty, any subsequent calls to
<code>get</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

When the channel is empty, return a signal.

@method channel[A].closeD() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Close the channel and return a signal immediately.
This has the effect of immediately causing any <link linkend="ref.concepts.states.block">blocked</link> calls to
<code>get</code> to <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. In addition, any subsequent calls to <code>put</code>
will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>, and once the channel becomes empty, any subsequent calls to
<code>get</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method channel[A].isClosed() :: Boolean

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If the channel is currently closed, return true, otherwise return false.

@method channel[A].getAll() :: List[A]

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Get all of the items currently in the channel, emptying the channel and returning
a list of the items in the order they were added. If there are no items in the
channel, return an empty list.
--}
import site Channel = "orc.lib.state.Channel"
import type Channel = "orc.lib.state.types.ChannelType"


{--
@site BoundedChannel[A](Integer) :: BoundedChannel[A]
Create a new asynchronous FIFO channel with the given number of slots.
Putting an item into the channel fills a slot, and getting an item opens a slot.
A channel with zero slots is equivalent to a synchronous channel.

A bounded channel may be empty, partly filled, or full, and either open or
closed.  When empty and open, calls to <code>get</code> <link linkend="ref.concepts.states.block">block</link>.  When empty and
closed, calls to <code>get</code> <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. When full and open, calls to
<code>put</code> <link linkend="ref.concepts.states.block">block</link>. When closed, calls to <code>put</code> <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.  In all
other cases, calls return normally.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "Put 1" "Got 1" "Put 2" "Got 2"
val c = BoundedChannel(1)
  c.put(1) >> "Put " + 1
| c.put(2) >> "Put " + 2
| Rwait(1000) >> (
    c.get() >n> "Got " + n
  | c.get() >n> "Got " + n
  )]]></programlisting>

@method boundedChannel[A].get() :: A

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Get an item from the channel. If the channel is open and no items are available,
<link linkend="ref.concepts.states.block">block</link> until one becomes available. If the channel is <link
linkend="ref.stdlib.state.BoundedChannel.close">closed</link> and no items are available, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method boundedChannel[A].getD() :: A

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Get an item from the channel. If no items are available, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method boundedChannel[A].put(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Put an item in the channel. If no slots are open, <link linkend="ref.concepts.states.block">block</link> until one becomes open.
If the channel is <link linkend="ref.stdlib.state.BoundedChannel.close">closed</link>, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method boundedChannel[A].putD(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Put an item in the channel. If no slots are open, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.
If the channel is <link linkend="ref.stdlib.state.BoundedChannel.close">closed</link>, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method boundedChannel[A].close() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

<firstterm xml:id="ref.stdlib.state.BoundedChannel.close">Close</firstterm> the channel and <link linkend="ref.concepts.states.block">block</link> until it is empty.
This has the effect of immediately causing any <link linkend="ref.concepts.states.block">blocked</link> calls to
<code>get</code> to <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. In addition, any subsequent calls to <code>put</code>
will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>, and once the channel becomes empty, any subsequent calls to
<code>get</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. Note that any <link linkend="ref.concepts.states.block">blocked</link> calls to <code>put</code>
initiated prior to closing the channel may still be allowed to return as usual.

@method boundedChannel[A].closeD() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Close the channel and return a signal immediately.
This has the effect of immediately causing any <link linkend="ref.concepts.states.block">blocked</link> calls to
<code>get</code> to <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. In addition, any subsequent calls to <code>put</code>
will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>, and once the channel becomes empty, any subsequent calls to
<code>get</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. Note that any <link linkend="ref.concepts.states.block">blocked</link> calls to <code>put</code>
initiated prior to closing the channel may still be allowed to return as usual.

@method boundedChannel[A].isClosed() :: Boolean

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If the channel is currently closed, return true, otherwise return false.

@method boundedChannel[A].getOpen() :: Integer

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Return the number of open slots in the channel. Because of concurrency
this value may become out-of-date so it should only be used for debugging
or statistical measurements.

@method boundedChannel[A].getBound() :: Integer

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

Return the total number of slots (open or filled) in the channel.

@method boundedChannel[A].getAll() :: List[A]

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Get all of the items currently in the channel or waiting to be added, emptying
the channel and returning a list of the items in the order they were added. If
there are no items in the channel or waiting to be added, return an empty list.
--}
import site BoundedChannel = "orc.lib.state.BoundedChannel"
import type BoundedChannel = "orc.lib.state.types.BoundedChannelType"


{--
@site Array[A](Integer) :: Array[A]
Create a new native array of the given size. The array is initialized
to contain <code>null</code>s.

The resulting array can be called directly with an index, as if
its type were <code>lambda (Integer) :: Ref[A]</code>.
In this case, it returns a <link linkend="ref.stdlib.state.Ref">Ref</link>
pointing to the element of the array specified by an index,
counting from 0. Changes to the array are reflected immediately
in the ref and visa versa.

Simple example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 3
val a = Array(1)
a(0) := 3 >>
a(0)?]]></programlisting>

More complex example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 1 2
val a = Array(3)
for(0, a.length?) >i>
a(i) := i >>
stop
; a(0)? | a(1)? | a(2)?]]></programlisting>

@method Array[A](Integer, String) :: Array[A]
Create a new primitive array of the given size with the given primitive type.
The initial values in the array depend on the primitive type: for numeric types,
it is <code>0</code>; for booleans, <code>false</code>; for chars, the character
with codepoint <code>0</code>.

The element type of the array should be the appropriate wrapper type for the given
primitive type, although a typechecker may not be able to verify this. This
constructor is only necessary when interfacing with certain Java libraries; most
programs will just use the  <code>Array(Integer)</code> constructor.

@method array[A].length? :: Integer

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

Return the size of the array.
--}
import site Array = "orc.lib.state.JavaArray"
import type Array = "orc.lib.state.types.ArrayType"


{--
@def Table[A](Integer, lambda (Integer) :: A)(Integer) :: A
The call <code>Table(n,f)</code>, where <code>n</code> is a
natural number and <code>f</code> a total function over natural numbers,
creates and returns a partial, pre-computed version of <code>f</code>
restricted to the range (0, <code>n</code>-1). <code>Table</code> does
not return a value until all calls to <code>f</code> have completed.
Consequently, if <code>f</code> <link linkend="ref.concepts.states.halt">halts</link> <link linkend="ref.concepts.silent">silently</link> on any call, the call to 
<code>Table</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

The user may also think of the call as returning an immutable array whose
<code>i</code>th element is accessed by calling <code>f(i)</code>.

This function provides a simple form of memoisation; we avoid recomputing
the value of <code>f(i)</code> by internally storing the result in an array.

Example:
<programlisting><![CDATA[
val a = Table(5, fib)
-- Publishes the 4th number of the fibonnaci sequence: 5
a(3)]]></programlisting>

@implementation
--}
def Table[A](Integer, lambda (Integer) :: A) :: (lambda(Integer) :: A)
def Table(n, f) =
  val a = Array[A](n) :: Array[A]
  def fill(Integer, lambda (Integer) :: A) :: Signal
  def fill(i, f) =
    if i <: 0 then signal
    else ((a(i) := f(i)), fill(i-1, f)) >> signal
  fill(n-1, f) >> (lambda (i :: Integer) = a(i)?)


{--
@site Counter(Integer) :: Counter
Create a new counter initialized to the given value.

@site Counter() :: Counter
Create a new counter initialized to zero.

@method counter.inc() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Increment the counter.

@method counter.dec() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If the counter is already at zero, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. Otherwise, decrement
the counter and return a signal.

@method counter.onZero() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

If the counter is at zero, return a signal. Otherwise,
<link linkend="ref.concepts.states.block">block</link> until the counter reaches zero.

@method counter.value() :: Integer

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Return the current value of the counter.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes five signals
val c = Counter(5)
repeat(c.dec)]]></programlisting>
--}
import site Counter = "orc.lib.state.Counter"
import type Counter = "orc.lib.state.types.CounterType"

{--
@site Dictionary() :: Dictionary
Create a new dictionary (a mutable map from field names to values), initially
empty.  The first time each field of the dictionary is accessed (using dot
notation), the dictionary creates and returns a new empty 
<link linkend="ref.stdlib.state.Ref">Ref</link> which will also be returned on
subsequent accesses of the same field.  Dictionaries allow you to easily create
object-like data structures.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Prints: 1 2
val d = Dictionary()
  Println(d.one.read()) >>
  Println(d.two.read()) >>
  stop
| d.one.write(1) >>
  d.two.write(2) >>
  stop]]></programlisting>

Here is the same example rewritten using Orc's reference syntax to improve
clarity:
<programlisting language="orc-demo"><![CDATA[
-- Prints: 1 2
val d = Dictionary()
  Println(d.one?) >>
  Println(d.two?) >>
  stop
| d.one := 1 >>
  d.two := 2 >>
  stop]]></programlisting>

To create a multi-level dictionary, you must explicitly create sub-dictionaries
for each field. For example:
<programlisting language="orc-demo"><![CDATA[
-- Prints: 2
val d = Dictionary()
d.one := Dictionary() >>
d.one?.two := 2 >>
Println(d.one?.two?) >>
stop]]></programlisting>

Note that you cannot write <code>d.one.two</code>: because <code>d.one</code>
is a reference to a dictionary, and not simply a dictionary, you must
dereference before accessing its fields, as in <code>d.one? >x> x.two</code>.
For readers familiar with the C language, this is the same reason you must
write <code language="c">s->field</code> instead of <code>s.field</code> when
<code>s</code> is a pointer to a struct.
--}
import site Dictionary = "orc.lib.state.Dictionary"

{--
@def fst[A,B]((A,B)) :: A
Return the first element of a pair.

@implementation
--}
def fst[A,B]((A,B)) :: A
def fst((x,_)) = x

{--
@def snd[A,B]((A,B)) :: B
Return the second element of a pair.

@implementation
--}
def snd[A,B]((A,B)) :: B
def snd((_,y)) = y

{--
@site Interval[A](A, A) :: Interval[A]
<code>Interval(a,b)</code> returns an object representing the half-open
interval [<code>a</code>,<code>b</code>).

@method interval[A].isEmpty() :: Boolean
Return true if this interval is empty.

@method interval[A].spans(A) :: Boolean
Return true if the interval spans the given point, false otherwise.

@method interval[A].intersects(Interval[A]) :: Boolean
Return true if the given interval has a non-empty intersection with this one,
and false otherwise.

@method interval[A].intersect(Interval[A]) :: Interval[A]
Return the intersection of this interval with another. If
the two intervals do not intersect, returns an empty interval.

@method interval[A].contiguous(Interval[A]) :: Boolean
Return true if the given interval is contiguous with this one
(overlaps or abuts), and false otherwise.

@method interval[A].union(Interval[A]) :: Interval[A]
Return the union of this interval with another. <link linkend="ref.concepts.states.halt">Halts</link> with an error if
the two intervals are not contiguous.
--}
import class Interval = "orc.lib.state.Interval"

{--
@site Intervals[A]() :: Intervals[A]
Return an empty set of intervals. An Intervals object is iterable;
iterating over the set returns disjoint intervals in increasing order.

@method intervals[A].isEmpty() :: Boolean
Return true if this set of intervals is empty.

@method intervals[A].spans(A) :: Boolean
Return true if this set of intervals spans the given point, and false otherwise.

@method intervals[A].intersect(Intervals[A]) :: Intervals[A]
Return the intersection of this set of intervals with another.

@method intervals[A].union(Interval[A]) :: Intervals[A]
Return the union of this set of intervals with the given interval.
This method is most efficient when the given interval is before
most of the intervals in the set.
--}
import class Intervals = "orc.lib.state.Intervals"

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
type List = "orc.lib.builtin.structured.ListType"

{--
@site Some[A](A) :: Option[A]
An optional value which is available.
This site may also be used in a pattern.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: (3,4)
Some((3,4)) >s> (
    s >Some((x,y))> (x,y)
  | s >None()> signal
)]]></programlisting>

@site None[A]() :: Option[A]
An optional value which is not available.
This site may also be used in a pattern.

--}
type Option = "orc.lib.builtin.structured.OptionType"
site Some = "orc.lib.builtin.structured.SomeSite"
site None = "orc.lib.builtin.structured.NoneSite"


{--
@site Cell[A]() :: Cell[A]
Create a write-once storage location.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 5 5
val c = Cell()
  c.write(5) >> c.read()
| Rwait(1) >> ( c.write(10) ; c.read() )]]></programlisting>

@method Cell[A].read() :: A

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Read a value from the cell. If the cell does not yet have a value, <link linkend="ref.concepts.states.block">block</link> until it receives one.

@method Cell[A].readD() :: A

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Read a value from the cell. If the cell does not yet have a value, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method Cell[A].write(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Write a value to the cell. If the cell already has a value, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.
--}
site Cell = "orc.lib.state.Cell"
type Cell = "orc.lib.state.types.CellType"


{--
@site Ref[A]() :: Ref[A]
Create a rewritable storage location without an initial value.

Example:
<programlisting language="orc-demo"><![CDATA[
val r = Ref()
Rwait(1000) >> r := 5 >> stop
| println(r?) >>
  r := 10 >>
  println(r?) >>
  stop]]></programlisting>

@method Ref[A](A) :: Ref[A]
Create a rewritable storage location initialized to the provided value.

@method Ref[A].read() :: A
Read the value of the ref. If the ref does not yet have a value, <link linkend="ref.concepts.states.block">block</link> until it receives one.

@method Ref[A].readD() :: A
Read the value of the ref. If the ref does not yet have a value, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method Ref[A].write(A) :: Signal
Write a value to the ref.

--}
site Ref = orc.lib.state.Ref
type Ref = orc.lib.state.types.RefType

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
Swap the values in two references.

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
println("Entering critical section") >>
println("Leaving critical section") >>
lock.release()]]></programlisting>

@method Semaphore.acquire() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

If the semaphore's value is greater than <code>0</code>, decrement the semaphore and return a signal.
If the semaphore's value is <code>0</code>, <link linkend="ref.concepts.states.block">block</link> until it becomes greater than <code>0</code>.

@method Semaphore.acquireD() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If the semaphore's value is greater than <code>0</code>, decrement the semaphore and return a signal.
If the semaphore's value is <code>0</code>, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method Semaphore.release() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>


If any calls to <code>acquire</code> are <link linkend="ref.concepts.states.block">blocked</link>, allow the oldest such call
to return.  Otherwise, increment the value of the semaphore.  This may
increment the value beyond that with which the semaphore was constructed.  

@method Semaphore.snoop() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

If any calls to <code>acquire</code> are <link linkend="ref.concepts.states.block">blocked</link>, return a signal.
Otherwise, <link linkend="ref.concepts.states.block">block</link> until some call to <code>acquire</code> <link linkend="ref.concepts.states.block">blocks</link>.

@method Semaphore.snoopD() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If any calls to <code>acquire</code> are <link linkend="ref.concepts.states.block">blocked</link>, return a signal.
Otherwise, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.
--}
site Semaphore = "orc.lib.state.Semaphore"
type Semaphore = "orc.lib.state.types.SemaphoreType"

{--
@site Buffer[A]() :: Buffer[A]
Create a new buffer (FIFO channel) of unlimited size. A buffer supports
get, put and close operations.

A buffer may be either empty or non-empty, and either open or closed.  When
empty and open, calls to <code>get</code> <link linkend="ref.concepts.states.block">block</link>.  When empty and closed, calls
to <code>get</code> <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.  When closed, calls to <code>put</code> <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.  In all
other cases, calls return normally.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 10
val b = Buffer()
  Rwait(1000) >> b.put(10) >> stop
| b.get()]]></programlisting>

@method Buffer[A].get() :: A

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Get an item from the buffer. If the buffer is open and no items are available,
<link linkend="ref.concepts.states.block">block</link> until one becomes available. If the buffer is <link
linkend="ref.stdlib.state.Buffer.close">closed</link> and no items are available, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method Buffer[A].getD() :: A

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Get an item from the buffer. If no items are available, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method Buffer[A].put(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Put an item in the buffer. If the buffer is <link
linkend="ref.stdlib.state.Buffer.close">closed</link>, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method Buffer[A].close() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

<firstterm xml:id="ref.stdlib.state.Buffer.close">Close</firstterm> the buffer and <link linkend="ref.concepts.states.block">block</link> until it is empty.
This has the effect of immediately causing any <link linkend="ref.concepts.states.block">blocked</link> calls to
<code>get</code> to <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. In addition, any subsequent calls to <code>put</code>
will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

When the buffer is empty, return a signal.

@method Buffer[A].closeD() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Close the buffer and return a signal immediately.
This has the effect of immediately causing any <link linkend="ref.concepts.states.block">blocked</link> calls to
<code>get</code> to <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. In addition, any subsequent calls to <code>put</code>
will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method Buffer[A].isClosed() :: Boolean

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If the buffer is currently closed, return true, otherwise return false.

@method Buffer[A].getAll() :: List[A]

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Get all of the items currently in the buffer, emptying the buffer and returning
a list of the items in the order they were added. If there are no items in the
buffer, return an empty list.
--}
site Buffer = "orc.lib.state.Buffer"
type Buffer = "orc.lib.state.types.BufferType"


{--
@site BoundedBuffer[A](Integer) :: BoundedBuffer[A]
Create a new buffer (FIFO channel) with the given number of slots.
Putting an item into the buffer fills a slot, and getting an item opens a slot.
A buffer with zero slots is equivalent to a synchronous channel.

A bounded buffer may be empty, partly filled, or full, and either open or
closed.  When empty and open, calls to <code>get</code> <link linkend="ref.concepts.states.block">block</link>.  When empty and
closed, calls to <code>get</code> <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. When full and open, calls to
<code>put</code> <link linkend="ref.concepts.states.block">block</link>. When closed, calls to <code>put</code> <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.  In all
other cases, calls return normally.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "Put 1" "Got 1" "Put 2" "Got 2"
val c = BoundedBuffer(1)
  c.put(1) >> "Put " + 1
| c.put(2) >> "Put " + 2
| Rwait(1000) >> (
    c.get() >n> "Got " + n
  | c.get() >n> "Got " + n
  )]]></programlisting>

@method BoundedBuffer[A].get() :: A

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Get an item from the buffer. If the buffer is open and no items are available,
<link linkend="ref.concepts.states.block">block</link> until one becomes available. If the buffer is <link
linkend="ref.stdlib.state.BoundedBuffer.close">closed</link> and no items are available, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method BoundedBuffer[A].getD() :: A

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Get an item from the buffer. If no items are available, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method BoundedBuffer[A].put(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Put an item in the buffer. If no slots are open, <link linkend="ref.concepts.states.block">block</link> until one becomes open.
If the buffer is <link linkend="ref.stdlib.state.BoundedBuffer.close">closed</link>, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method BoundedBuffer[A].putD(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Put an item in the buffer. If no slots are open, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.
If the buffer is <link linkend="ref.stdlib.state.BoundedBuffer.close">closed</link>, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.

@method BoundedBuffer[A].close() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

<firstterm xml:id="ref.stdlib.state.BoundedBuffer.close">Close</firstterm> the buffer and <link linkend="ref.concepts.states.block">block</link> until it is empty.
This has the effect of immediately causing any <link linkend="ref.concepts.states.block">blocked</link> calls to
<code>get</code> to <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. In addition, any subsequent calls to <code>put</code>
will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. Note that any <link linkend="ref.concepts.states.block">blocked</link> calls to <code>put</code>
initiated prior to closing the buffer may still be allowed to return as usual.

@method BoundedBuffer[A].closeD() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Close the buffer and return a signal immediately.
This has the effect of immediately causing any <link linkend="ref.concepts.states.block">blocked</link> calls to
<code>get</code> to <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. In addition, any subsequent calls to <code>put</code>
will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. Note that any <link linkend="ref.concepts.states.block">blocked</link> calls to <code>put</code>
initiated prior to closing the buffer may still be allowed to return as usual.

@method BoundedBuffer[A].isClosed() :: Boolean

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If the buffer is currently closed, return true, otherwise return false.

@method BoundedBuffer[A].getOpen() :: Integer

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Return the number of open slots in the buffer. Because of concurrency
this value may become out-of-date so it should only be used for debugging
or statistical measurements.

@method BoundedBuffer[A].getBound() :: Integer

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

Return the total number of slots (open or filled) in the buffer.

@method BoundedBuffer[A].getAll() :: [A]

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Get all of the items currently in the buffer or waiting to be added, emptying
the buffer and returning a list of the items in the order they were added. If
there are no items in the buffer or waiting to be added, return an empty list.
--}
site BoundedBuffer = orc.lib.state.BoundedBuffer
type BoundedBuffer = orc.lib.state.types.BoundedBufferType


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

@method Array[A].get(Integer) :: A

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Get the element of the array given by the index, counting from 0.
<code>a.get(i)</code> is equivalent to <code>a(i)?</code>.

@method Array[A].set(Integer, A) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Set the element of the array given by the index, counting from 0.
<code>a.set(i,v)</code> is equivalent to <code>a(i) := v</code>.

@method Array[A].slice(Integer, Integer) :: Array[A]

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

Return a copy of the portion of the array with indices covered by the given
half-open range. The result array is still indexed counting from 0.

@method Array[A].length? :: Integer

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

Return the size of the array.

@method Array[A].fill(A) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Set every element of the array to the given value. The given value is not
copied, but is shared by every element of the array, so for example
<code>a.fill(Semaphore(1))</code> would allow you to access the same semaphore
from every element <code>a</code>.

This method is primarily useful to initialize or reset an array to a constant
value,
for example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 0 0
val a = Array(3)
a.fill(0) >> each(a)]]></programlisting>
--}
site Array = "orc.lib.state.JavaArray"
type Array = "orc.lib.state.types.ArrayType"

--TODO: define former array methods: get set slice fill length

def slice[A](Array[A], Integer, Integer) :: Top
def slice(xs, from, until) =
  class ScalaPredef = "scala.Predef"
  ScalaPredef.refArrayOps[A](xs).slice(from, until)


def fill[A](Array[Top], A) :: Signal
def fill(a, x) =
  def fillBelow(Integer) :: Signal
  def fillBelow(i) =
    if i <: 0 then signal
    else ((a(i) := x), fillBelow(i-1)) >> signal
  fillBelow(a.length?-1)

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
def Table[A](Integer, lambda (Integer) :: A)(Integer) :: A
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

@method Counter() :: Counter
Create a new counter initialized to zero.

@method Counter.inc() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Increment the counter.

@method Counter.dec() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

If the counter is already at zero, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>. Otherwise, decrement
the counter and return a signal.

@method Counter.onZero() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

If the counter is at zero, return a signal. Otherwise,
<link linkend="ref.concepts.states.block">block</link> until the counter reaches zero.

@method Counter.value() :: Integer

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
site Counter = "orc.lib.state.Counter"
type Counter = "orc.lib.state.types.CounterType"

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
  println(d.one.read()) >>
  println(d.two.read()) >>
  stop
| d.one.write(1) >>
  d.two.write(2) >>
  stop]]></programlisting>

Here is the same example rewritten using Orc's reference syntax to improve
clarity:
<programlisting language="orc-demo"><![CDATA[
-- Prints: 1 2
val d = Dictionary()
  println(d.one?) >>
  println(d.two?) >>
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
println(d.one?.two?) >>
stop]]></programlisting>

Note that you cannot write <code>d.one.two</code>: because <code>d.one</code>
is a reference to a dictionary, and not simply a dictionary, you must
dereference before accessing its fields, as in <code>d.one? >x> x.two</code>.
For readers familiar with the C language, this is the same reason you must
write <code language="c">s->field</code> instead of <code>s.field</code> when
<code>s</code> is a pointer to a struct.
--}
site Dictionary = orc.lib.state.Dictionary

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

@method Interval[A].isEmpty() :: Boolean
Return true if this interval is empty.

@method Interval[A].spans(A) :: Boolean
Return true if the interval spans the given point, false otherwise.

@method Interval[A].intersects(Interval[A]) :: Boolean
Return true if the given interval has a non-empty intersection with this one,
and false otherwise.

@method Interval[A].intersect(Interval[A]) :: Interval[A]
Return the intersection of this interval with another. If
the two intervals do not intersect, returns an empty interval.

@method Interval[A].contiguous(Interval[A]) :: Boolean
Return true if the given interval is contiguous with this one
(overlaps or abuts), and false otherwise.

@method Interval[A].union(Interval[A]) :: Interval[A]
Return the union of this interval with another. <link linkend="ref.concepts.states.halt">Halts</link> with an error if
the two intervals are not contiguous.
--}
class Interval = "orc.lib.state.Interval"

{--
@site Intervals[A]() :: Intervals[A]
Return an empty set of intervals. An Intervals object is iterable;
iterating over the set returns disjoint intervals in increasing order.

@method Intervals[A].isEmpty() :: Boolean
Return true if this set of intervals is empty.

@method Intervals[A].spans(A) :: Boolean
Return true if this set of intervals spans the given point, and false otherwise.

@method Intervals[A].intersect(Intervals[A]) :: Intervals[A]
Return the intersection of this set of intervals with another.

@method Intervals[A].union(Interval[A]) :: Intervals[A]
Return the union of this set of intervals with the given interval.
This method is most efficient when the given interval is before
most of the intervals in the set.
--}
class Intervals = "orc.lib.state.Intervals"

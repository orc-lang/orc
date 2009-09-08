{--
General-purpose supplemental data structures.
--}

-- Make the List type constructor available
type List = orc.type.structured.ListType

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
type Option = orc.type.structured.OptionType
site Some = orc.runtime.sites.core.Some
site None = orc.runtime.sites.core.None

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

@method Semaphore.acquire() :: Top
If the semaphore's value is greater than <code>0</code>, decrement the semaphore and return a signal.
If the semaphore's value is <code>0</code>, block until it becomes greater than <code>0</code>.

@method Semaphore.acquirenb() :: Top
If the semaphore's value is greater than <code>0</code>, decrement the semaphore and return a signal.
If the semaphore's value is <code>0</code>, halt.

@method Semaphore.release() :: Top
If any calls to <code>acquire</code> are blocked, allow the oldest such call
to return.  Otherwise, increment the value of the semaphore.  This may
increment the value beyond that with which the semaphore was constructed.  

@method Semaphore.snoop() :: Top
If any calls to <code>acquire</code> are blocked, return a signal.
Otherwise, block until some call to <code>acquire</code> blocks.

@method Semaphore.snoopnb() :: Top
If any calls to <code>acquire</code> are blocked, return a signal.
Otherwise, halt.
--}
site Semaphore = orc.lib.state.Semaphore
type Semaphore = orc.lib.state.types.SemaphoreType

{--
@site Buffer[A]() :: Buffer[A]
<!-- XSL/FO doesn't like me using an anchor tag here -->
<para id="orc.lib.Buffer.close">
Create a new buffer (FIFO channel) of unlimited size. A buffer supports
get, put and close operations.
</para>

A buffer may be either empty or non-empty, and either open or closed.  When
empty and open, calls to <code>get</code> block.  When empty and closed, calls
to <code>get</code> halt.  When closed, calls to <code>put</code> halt.  In all
other cases, calls return normally.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 10
val b = Buffer()
  Rtimer(1000) >> b.put(10) >> stop
| b.get()]]></programlisting>

@method Buffer[A].get() :: A
Get an item from the buffer. If the buffer is open and no items are available,
block until one becomes available. If the buffer is <link
linkend="orc.lib.Buffer.close">closed</link> and no items are available, halt.

@method Buffer[A].getnb() :: A
Get an item from the buffer. If no items are available, halt.

@method Buffer[A].put(A) :: Top
Put an item in the buffer. If the buffer is <link
linkend="orc.lib.Buffer.close">closed</link>, halt.

@method Buffer[A].close() :: Top
Close the buffer and block until it is empty.
This has the effect of immediately causing any blocked calls to
<code>get</code> to halt. In addition, any subsequent calls to <code>put</code>
will halt, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will halt.

@method Buffer[A].closenb() :: Top
Close the buffer and return a signal immediately.
This has the effect of immediately causing any blocked calls to
<code>get</code> to halt. In addition, any subsequent calls to <code>put</code>
will halt, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will halt.

@method Buffer[A].isClosed() :: Boolean
If the buffer is currently closed, return true, otherwise return false.

@method Buffer[A].getAll() :: List[A]
Get all of the items currently in the buffer, emptying the buffer and returning
a list of the items in the order they were added. If there are no items in the
buffer, return an empty list.
--}
site Buffer = orc.lib.state.Buffer
type Buffer = orc.lib.state.types.BufferType

{--
@site BoundedBuffer[A](Integer) :: BoundedBuffer[A]
Create a new buffer (FIFO channel) with the given number of slots.
Putting an item into the buffer fills a slot, and getting an item opens a slot.
A buffer with zero slots is equivalent to a
<link linkend="orc.lib.state.SyncChannel">synchronous channel</link>.

A bounded buffer may be empty, partly filled, or full, and either open or
closed.  When empty and open, calls to <code>get</code> block.  When empty and
closed, calls to <code>get</code> halt. When full and open, calls to
<code>put</code> block. When closed, calls to <code>put</code> halt.  In all
other cases, calls return normally.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "Put 1" "Got 1" "Put 2" "Got 2"
val c = BoundedBuffer(1)
  c.put(1) >> "Put " + 1
| c.put(2) >> "Put " + 2
| Rtimer(1000) >> (
    c.get() >n> "Got " + n
  | c.get() >n> "Got " + n
  )]]></programlisting>

@method BoundedBuffer[A].get() :: A
Get an item from the buffer. If the buffer is open and no items are available,
block until one becomes available. If the buffer is <link
linkend="orc.lib.BoundedBuffer.close">closed</link> and no items are available, halt.

@method BoundedBuffer[A].getnb() :: A
Get an item from the buffer. If no items are available, halt.

@method BoundedBuffer[A].put(A) :: Top
Put an item in the buffer. If no slots are open, block until one becomes open.
If the buffer is <link linkend="orc.lib.BoundedBuffer.close">closed</link>, halt.

@method BoundedBuffer[A].putnb(A) :: Top
Put an item in the buffer. If no slots are open, halt.
If the buffer is <link linkend="orc.lib.BoundedBuffer.close">closed</link>, halt.

@method BoundedBuffer[A].close() :: Top
<!-- XSL/FO doesn't like me using an anchor tag here -->
<para id="orc.lib.BoundedBuffer.close">
Close the buffer and block until it is empty.
This has the effect of immediately causing any blocked calls to
<code>get</code> to halt. In addition, any subsequent calls to <code>put</code>
will halt, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will halt. Note that any blocked calls to <code>put</code>
initiated prior to closing the buffer may still be allowed to return as usual.
</para>

@method BoundedBuffer[A].closenb() :: Top
Close the buffer and return a signal immediately.
This has the effect of immediately causing any blocked calls to
<code>get</code> to halt. In addition, any subsequent calls to <code>put</code>
will halt, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will halt. Note that any blocked calls to <code>put</code>
initiated prior to closing the buffer may still be allowed to return as usual.

@method BoundedBuffer[A].isClosed() :: Boolean
If the buffer is currently closed, return true, otherwise return false.

@method BoundedBuffer[A].getOpen() :: Integer
Return the number of open slots in the buffer. Because of concurrency
this value may become out-of-date so it should only be used for debugging
or statistical measurements.

@method BoundedBuffer[A].getBound() :: Integer
Return the total number of slots (open or filled) in the buffer.

@method BoundedBuffer[A].getAll() :: [A]
Get all of the items currently in the buffer or waiting to be added, emptying
the buffer and returning a list of the items in the order they were added. If
there are no items in the buffer or waiting to be added, return an empty list.
--}
site BoundedBuffer = orc.lib.state.BoundedBuffer
type BoundedBuffer = orc.lib.state.types.BoundedBufferType

{--
@site SyncChannel[A]() :: SyncChannel[A]
<!-- XSL/FO doesn't like me using an anchor tag here -->
<para id="orc.lib.state.SyncChannel">
Create a synchronous channel, or rendezvous.
</para>

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publish: 10
val c = SyncChannel()
  c.put(10)
| Rtimer(1000) >> c.get()]]></programlisting>

@method SyncChannel[A].get() :: A
Receive an item over the channel. If no sender is available, block until one becomes available.

@method SyncChannel[A].put(A) :: Top
Send an item over the channel. If no receiver is available, block until one becomes available.
--}
site SyncChannel = orc.lib.state.SyncChannel
type SyncChannel = orc.lib.state.types.SyncChannelType

{--
@site Cell[A]() :: Cell[A]
Create a write-once storage location.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 5 5
val c = Cell()
  c.write(5) >> c.read()
| Rtimer(1) >> ( c.write(10) ; c.read() )]]></programlisting>

@method Cell[A].read() :: A
Read a value from the cell. If the cell does not yet have a value, block until it receives one.

@method Cell[A].readnb() :: A
Read a value from the cell. If the cell does not yet have a value, halt.

@method Cell[A].write() :: Top
Write a value to the cell. If the cell already has a value, halt.
--}
site Cell = orc.lib.state.Cell
type Cell = orc.lib.state.types.CellType

{--
@site Ref[A]() :: Ref[A]
<para id="orc.lib.state.Ref">
Create a rewritable storage location without an initial value.
</para>

Example:
<programlisting language="orc-demo"><![CDATA[
val r = Ref()
Rtimer(1000) >> r := 5 >> stop
| println(r?) >>
  r := 10 >>
  println(r?) >>
  stop]]></programlisting>

@method Ref[A](A) :: Ref[A]
Create a rewritable storage location initialized to the provided value.

@method Ref[A].read() :: A
Read the value of the ref. If the ref does not yet have a value, block until it receives one.

@method Ref[A].readnb() :: A
Read the value of the ref. If the ref does not yet have a value, halt.

@method Ref[A].write(A) :: Top
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
@def (:=)[A](Ref[A], A) :: Top
Set the value held by a reference.
<code>x := y</code> is equivalent to <code>x.write(y)</code>.

@implementation
--}
def (:=)[A](Ref[A], A) :: Top
def (:=)(r,v) = r.write(v)

{--
@def swap[A](Ref[A], Ref[A]) :: Top
Swap the values in two references.

@implementation
--}
def swap[A](Ref[A], Ref[A]) :: Top
def swap(r,s) = (r?,s?) >(rval,sval)> (r := sval, s := rval) >> signal


{--
@site Array[A](Integer) :: Array[A]
Create a new native array of the given size. The array is initialized
to contain <code>null</code>s.

The resulting array can be called directly with an index, as if
its type were <code>lambda (Integer) :: Ref[A]</code>.
In this case, it returns a <link linkend="orc.lib.state.Ref">Ref</link>
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
for(0, a.length()) >i>
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
Get the element of the array given by the index, counting from 0.
<code>a.get(i)</code> is equivalent to <code>a(i)?</code>.

@method Array[A].set(Integer, A) :: Top
Set the element of the array given by the index, counting from 0.
<code>a.set(i,v)</code> is equivalent to <code>a(i) := v</code>.

@method Array[A].slice(Integer, Integer) :: Array[A]
Return a copy of the portion of the array with indices covered by the given
half-open range. The result array is still indexed counting from 0.

@method Array[A].length() :: Integer
Return the size of the array.

@method Array[A].fill(A) :: Top
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
site Array = orc.lib.util.JavaArray
type Array = orc.lib.state.types.ArrayType

{--
@def IArray[A](Integer, lambda (Integer) :: A)(Integer) :: A
The call <code>IArray(n,f)</code>, where <code>n</code> is a
natural number and <code>f</code> a total function over natural numbers,
creates and returns a partial, pre-computed version of <code>f</code>
restricted to the range (0, <code>n</code>-1). If <code>f</code> halts
on any number in this range, the call to <code>IArray</code> will halt.

The user may also think of the call as returning an array whose
<code>i</code>th element is <code>f(i)</code>.

This function provides a simple form of memoisation; we avoid recomputing
the value of <code>f(i)</code> by storing the result in an array.

Example:
<programlisting><![CDATA[
val a = IArray(5, fib)
-- Publishes the 4th number of the fibonnaci sequence: 5
a(3)]]></programlisting>

@implementation
--}
def IArray[A](Integer, lambda (Integer) :: A)(Integer) :: A
def IArray(n, f) =
  val a = Array[A](n)
  def fill(Integer, lambda (Integer) :: A) :: Top
  def fill(i, f) =
    if i < 0 then signal
    else (a.set(i, f(i)), fill(i-1, f)) >> signal
  fill(n-1, f) >> a.get


{--
@site Set[A]() :: Set[A]
Construct an empty mutable set. The set considers two
values <code>a</code> and <code>b</code> to be the same if
and only if <code>a=b</code>. This site conforms to the Java interface
<code>java.util.Set</code>, except that it obeys Orc rules for equality of
elements rather than Java rules.

@method Set[A].add(A) :: Boolean
Add a value to the set, returning true if the set did not already contain the value,
and false otherwise.

@method Set[A].remove(Top) :: Boolean
Remove a value from the set, returning true if the set contained the value,
and false otherwise.

@method Set[A].contains(Top) :: Boolean
Return true if the set contains the given value, and false otherwise.

@method Set[A].isEmpty() :: Boolean
Return true if the set contains no values.

@method Set[A].clear() :: Top
Remove all values from the set.

@method Set[A].size() :: Integer
Return the number of unique values currently contained in the set.
--}
class Set = orc.lib.state.Set

{--
@site Map[K,V]() :: Map[K,V]
Construct an empty mutable map from keys to values.  Each key contained in the
map is associated with exactly one value.  The mapping considers two keys
<code>a</code> and <code>b</code> to be the same if and only if
<code>a=b</code>. This site conforms to the Java interface
<code>java.util.Map</code>, except that it obeys Orc rules for equality of keys
rather than Java rules.

@method Map[K,V].put(K, V) :: V
<code>map.put(k,v)</code> associates the value <code>v</code> with the key
<code>k</code> in <code>map</code>, such that <code>map.get(k)</code> returns
<code>v</code>. Return the value previously associated with the key,
if any, otherwise return <code>Null()</code>.

@method Map[K,V].get(K) :: V
Return the value currently associated with the given key, if any, otherwise
return <code>Null()</code>.

@method Map[K,V].remove(Top) :: V
Remove the given key from the map. Return the value previously associated with the key,
if any, otherwise return <code>Null()</code>.

@method Map[K,V].containsKey(Top) :: Boolean
Return true if the map contains the given key, and false otherwise.

@method Map[K,V].isEmpty() :: Boolean
Return true if the map contains no keys.

@method Map[K,V].clear() :: Top
Remove all keys from the map.

@method Map[K,V].size() :: Integer
Return the number of unique keys currently contained in the map.
--}
class Map = orc.lib.state.Map

{--
@site Counter(Integer) :: Counter
Create a new counter initialized to the given value.

@method Counter() :: Counter
Create a new counter initialized to zero.

@method Counter.inc() :: Top
Increment the counter.

@method Counter.dec() :: Top
If the counter is already at zero, halt. Otherwise, decrement
the counter and return a signal.

@method Counter.onZero() :: Top
If the counter is at zero, return a signal. Otherwise
block until the counter reaches zero.

@method Counter.value() :: Integer
Return the current value of the counter.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes five signals
val c = Counter(5)
repeat(c.dec)]]></programlisting>
--}
site Counter = orc.lib.state.Counter
type Counter = orc.lib.state.types.CounterType

{--
@site Dictionary() :: Dictionary
Create a new dictionary (a mutable map from field names to values), initially
empty.  The first time each field of the dictionary is accessed (using dot
notation), the dictionary creates and returns a new empty <link
linkend="orc.lib.state.Ref">Ref</link> which will also be returned on
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
@site Record(String, A, String, B, ...) :: Record[A, B, ...]

Create a new record (an immutable map from field names to values).
Arguments are consumed in pairs; the first argument of each pair
is the key, and the second is the value for that key.

To access the value in record <code>r</code> for key <code>"x"</code>,
use the syntax <code>r.x</code>. For example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 1
val r = Record(
  "one", 1,
  "two", 2)
r.one]]></programlisting>
--}
site Record = orc.lib.state.Record

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
Return the union of this interval with another. Halts with an error if
the two intervals are not contiguous.
--}
class Interval = orc.lib.state.Interval

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
class Intervals = orc.lib.state.Intervals

{--
General-purpose supplemental data structures.
--}

{--
* site Semaphore(Integer) :: Semaphore
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

** site Semaphore.acquire() :: Signal
If the semaphore's value is greater than <code>0</code>, decrement the semaphore and return a signal.
If the semaphore's value is <code>0</code>, block until it becomes greater than <code>0</code>.

** site Semaphore.acquirenb() :: Signal
If the semaphore's value is greater than <code>0</code>, decrement the semaphore and return a signal.
If the semaphore's value is <code>0</code>, halt.

** site Semaphore.release() :: Signal
If any calls to <code>acquire</code> are blocked, allow the oldest such call
to return.  Otherwise, increment the value of the semaphore.  This may
increment the value beyond that with which the semaphore was constructed.  

** site Semaphore.snoop() :: Signal
If any calls to <code>acquire</code> are blocked, return a signal.
Otherwise, block until some call to <code>acquire</code> blocks.

** site Semaphore.snoopnb() :: Signal
If any calls to <code>acquire</code> are blocked, return a signal.
Otherwise, halt.
--}
site Semaphore = orc.lib.state.Semaphore

{--
* site Buffer() :: Buffer<A>
Create a new buffer (FIFO channel) of unlimited size. A buffer supports
get, put and close operations.

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

** site Buffer<A>.get() :: A
Get an item from the buffer. If the buffer is open and no items are available,
block until one becomes available. If the buffer is <link
linkend="orc.lib.Buffer.close">closed</link> and no items are available, halt.

Recall that the type signature <code>site Buffer&lt;A&gt;.get() :: A</code> means that
when the <code>get</code> method is called on a buffer holding an arbitrary
element type <code>A</code>, it will return a value of the same type.

** site Buffer<A>.getnb() :: A
Get an item from the buffer. If no items are available, halt.

** site Buffer<A>.put(A) :: Signal
Put an item in the buffer. If the buffer is <link
linkend="orc.lib.Buffer.close">closed</link>, halt.

** site Buffer<A>.close() :: Signal
Close the buffer and block until it is empty.
This has the effect of immediately causing any blocked calls to
<code>get</code> to halt. In addition, any subsequent calls to <code>put</code>
will halt, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will halt.

** site Buffer<A>.closenb() :: Signal
Close the buffer and return a signal immediately.
This has the effect of immediately causing any blocked calls to
<code>get</code> to halt. In addition, any subsequent calls to <code>put</code>
will halt, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will halt.

** site Buffer<A>.isClosed() :: Boolean
If the buffer is currently closed, return true, otherwise return false.

** site Buffer<A>.getAll() :: [A]
Get all of the items currently in the buffer, emptying the buffer and returning
a list of the items in the order they were added. If there are no items in the
buffer, return an empty list.
--}
site Buffer = orc.lib.state.Buffer

{--
* site BoundedBuffer(Integer) :: BoundedBuffer<A>
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

** site BoundedBuffer<A>.get() :: A
Get an item from the buffer. If the buffer is open and no items are available,
block until one becomes available. If the buffer is <link
linkend="orc.lib.Buffer.close">closed</link> and no items are available, halt.

** site BoundedBuffer<A>.getnb() :: A
Get an item from the buffer. If no items are available, halt.

** site BoundedBuffer<A>.put(A) :: Signal
Put an item in the buffer. If no slots are open, block until one becomes open.
If the buffer is <link linkend="orc.lib.Buffer.close">closed</link>, halt.

** site BoundedBuffer<A>.putnb(A) :: Signal
Put an item in the buffer. If no slots are open, halt.
If the buffer is <link linkend="orc.lib.Buffer.close">closed</link>, halt.

** site BoundedBuffer<A>.close() :: Signal
Close the buffer and block until it is empty.
This has the effect of immediately causing any blocked calls to
<code>get</code> to halt. In addition, any subsequent calls to <code>put</code>
will halt, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will halt. Note that any blocked calls to <code>put</code>
initiated prior to closing the buffer may still be allowed to return as usual.

** site BoundedBuffer<A>.closenb() :: Signal
Close the buffer and return a signal immediately.
This has the effect of immediately causing any blocked calls to
<code>get</code> to halt. In addition, any subsequent calls to <code>put</code>
will halt, and once the buffer becomes empty, any subsequent calls to
<code>get</code> will halt. Note that any blocked calls to <code>put</code>
initiated prior to closing the buffer may still be allowed to return as usual.

** site BoundedBuffer<A>.isClosed() :: Boolean
If the buffer is currently closed, return true, otherwise return false.

** site BoundedBuffer<A>.getOpen() :: Integer
Return the number of open slots in the buffer. Because of concurrency
this value may become out-of-date so it should only be used for debugging
or statistical measurements.

** site BoundedBuffer<A>.getBound() :: Integer
Return the total number of slots (open or filled) in the buffer.

** site BoundedBuffer<A>.getAll() :: [A]
Get all of the items currently in the buffer or waiting to be added, emptying
the buffer and returning a list of the items in the order they were added. If
there are no items in the buffer or waiting to be added, return an empty list.
--}
site BoundedBuffer = orc.lib.state.BoundedBuffer

{--
* site SyncChannel() :: SyncChannel<A>
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

** site SyncChannel<A>.get() :: A
Receive an item over the channel. If no sender is available, block until one becomes available.

** site SyncChannel<A>.put(A) :: Signal
Send an item over the channel. If no receiver is available, block until one becomes available.
--}
site SyncChannel = orc.lib.state.SyncChannel

{--
* site Cell() :: Cell<A>
Create a write-once storage location.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 5 5
val c = Cell()
  c.write(5) >> c.read()
| Rtimer(1) >> ( c.write(10) ; c.read() )]]></programlisting>

** site Cell<A>.read() :: A
Read a value from the cell. If the cell does not yet have a value, block until it receives one.

** site Cell<A>.readnb() :: A
Read a value from the cell. If the cell does not yet have a value, halt.

** site Cell<A>.write() :: Signal
Write a value to the cell. If the cell already has a value, halt.
--}
site Cell = orc.lib.state.Cell

{--
* site Ref() :: Ref<A>
<para id="orc.lib.state.Ref">
Create a rewritable storage location without an initial value.
</para>

Example:
<programlisting language="orc-demo"><![CDATA[
val r = Ref()
Rtimer(1000) >> r.write(5) >> stop
| println(r.read()) >>
  r.write(10) >>
  println(r.read()) >>
  stop]]></programlisting>

** site Ref(A) :: Ref<A>
Create a rewritable storage location initialized to the provided value.

** site Ref<A>.read() :: A
Read the value of the ref. If the ref does not yet have a value, block until it receives one.

** site Ref<A>.readnb() :: A
Read the value of the ref. If the ref does not yet have a value, halt.

** site Ref<A>.write(A) :: Signal
Write a value to the ref.

--}
site Ref = orc.lib.state.Ref

{--
** def (?)(Ref<A>) :: A
Get the value held by a reference.
<code>x?</code> is equivalent to <code>x.read()</code>.
--}
def (?)(r) = r.read()

{--
** def (:=)(Ref<A>, A) :: Signal
Set the value held by a reference.
<code>x := y</code> is equivalent to <code>x.write(y)</code>.
--}
def (:=)(r,v) = r.write(v)

{--
* site Array(Integer) :: Array<A>
Create a new native array of the given size.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 1 2
val a = Array(3)
for(0, a.length()) >i>
a.set(i, f(i)) >>
stop
; a.get(0) | a.get(1) | a.get(2)]]></programlisting>

** site Array(Integer, String) :: Array<A>
Create a new primitive array of the given size with the given primitive type.
The primitive type should match the element type of the array, although a
typechecker may not be able to verify this. This constructor is only necessary
when interfacing with certain Java libraries; most programs will just use the 
<code>Array(Integer)</code> constructor.

** site Array<A>.get(Integer) :: A
Get the element of the array given by the index, counting from 0.

** site Array<A>.set(Integer, A) :: Signal
Set the element of the array given by the index, counting from 0.

** site Array<A>.slice(Integer, Integer) :: Array<A>
Return a copy of the portion of the array with indices covered by the given
half-open range. The result array is still indexed counting from 0.

** site Array<A>.length() :: Integer
Return the size of the array.

** site Array<A>.fill(A) :: Signal
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

{--
* def IArray(Integer, lambda (Integer) :: A)(Integer) :: A
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
--}
def IArray(n, f) =
  val a = Array(n)
  def fill(i, f) =
    if i < 0 then []
    else a.set(i, f(i)):fill(i-1, f)
  fill(n-1, f) >> a.get

{--
* site Some(A) :: Option<A>
An optional value which is available.
This site may also be used in a pattern.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: (3,4)
Some((3,4)) >s> (
    s >Some((x,y))> (x,y)
  | s >None()> signal
)]]></programlisting>

* site None() :: Option<A>
An optional value which is not available.
This site may also be used in a pattern.

--}
type Option = Some(_) | None()

{--
* site Set() :: Set<A>
Construct an empty mutable set. The set considers two
values <code>a</code> and <code>b</code> to be the same if
and only if <code>a=b</code>. This site conforms to the Java interface
<code>java.util.Set</code>, except that it obeys Orc rules for equality of
elements rather than Java rules.

** site Set<A>.add(A) :: Boolean
Add a value to the set, returning true if the set did not already contain the value,
and false otherwise.

** site Set<A>.remove(Top) :: Boolean
Remove a value from the set, returning true if the set contained the value,
and false otherwise.

** site Set<A>.contains(Top) :: Boolean
Return true if the set contains the given value, and false otherwise.

** site Set<A>.isEmpty() :: Boolean
Return true if the set contains no values.

** site Set<A>.clear() :: Signal
Remove all values from the set.

** site Set<A>.size() :: Integer
Return the number of unique values currently contained in the set.
--}
class Set = orc.lib.data.Set

{--
* site Map() :: Map<K,V>
Construct an empty mutable map from keys to values.  Each key contained in the
map is associated with exactly one value.  The mapping considers two keys
<code>a</code> and <code>b</code> to be the same if and only if
<code>a=b</code>. This site conforms to the Java interface
<code>java.util.Map</code>, except that it obeys Orc rules for equality of keys
rather than Java rules.

** site Map<K,V>.put(K, V) :: V
<code>map.put(k,v)</code> associates the value <code>v</code> with the key
<code>k</code> in <code>map</code>, such that <code>map.get(k)</code> returns
<code>v</code>. Return the value previously associated with the key,
if any, otherwise return <code>Null()</code>.

** site Map<K,V>.get(K) :: V
Return the value currently associated with the given key, if any, otherwise
return <code>Null()</code>.

** site Map<K,V>.remove(Top) :: V
Remove the given key from the map. Return the value previously associated with the key,
if any, otherwise return <code>Null()</code>.

** site Map<K,V>.containsKey(Top) :: Boolean
Return true if the map contains the given key, and false otherwise.

** site Map<K,V>.isEmpty() :: Boolean
Return true if the map contains no keys.

** site Map<K,V>.clear() :: Signal
Remove all keys from the map.

** site Map<K,V>.size() :: Integer
Return the number of unique keys currently contained in the map.
--}
class Map = orc.lib.data.Map


{--
* site Counter(Integer) :: Counter
Create a new counter initialized to the given value.

** site Counter() :: Counter
Create a new counter initialized to zero.

** site Counter.inc() :: Signal
Increment the counter.

** site Counter.dec() :: Signal
If the counter is already at zero, halt. Otherwise, decrement
the counter and return a signal.

** site Counter.onZero() :: Signal
If the counter is at zero, return a signal. Otherwise
block until the counter reaches zero.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes five signals
val c = Counter(5)
repeat(c.dec)]]></programlisting>
--}
site Counter = orc.lib.state.Counter

{--
* site Dictionary() :: Dictionary
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
site Dictionary = orc.lib.data.Dictionary

{--
* def fst((A,B)) :: A
Return the first element of a pair.
--}
def fst((x,_)) = x

{--
* def snd((A,B)) :: B
Return the second element of a pair.
--}
def snd((_,y)) = y

{--
* def swap((A,B)) :: (B,A)
Swap the elements of a pair.
--}
def swap((x,y)) = (y,x)

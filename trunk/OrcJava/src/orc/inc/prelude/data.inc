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

--}
site Semaphore = orc.lib.state.Semaphore

{--
* site Buffer() :: Buffer<A>
Create a new buffer (FIFO channel) of unlimited size.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 10
val b = Buffer()
  Rtimer(1000) >> b.put(10) >> stop
| b.get()]]></programlisting>

** site Buffer<A>.get() :: A
Get an item from the buffer. If no items are available, block until one becomes available.

Recall that the type signature <code>site Buffer&lt;A&gt;.get() :: A</code> means that
when the <code>get</code> method is called on a buffer holding an arbitrary
element type <code>A</code>, it will return a value of the same type.

** site Buffer<A>.getnb() :: A
Get an item from the buffer. If no items are available, halt.

** site Buffer<A>.put(A) :: Signal
Put an item in the buffer.

** site Buffer<A>.close() :: Signal
Close the buffer. This has the effect of immediately causing any blocked calls
to <code>get</code> to halt. In addition, any subsequent calls to
<code>get</code> or <code>put</code> will halt.

** site Buffer<A>.isClosed() :: Boolean
If the buffer is currently closed, return true, otherwise return false.
--}
site Buffer = orc.lib.state.Buffer

{--
* site BoundedBuffer(Integer) :: BoundedBuffer<A>
Create a new buffer (FIFO channel) with the given number of slots.
Putting an item into the buffer fills a slot, and removing an item opens a slot.
A buffer with zero slots is equivalent to a
<link linkend="orc.lib.state.SyncChannel">synchronous channel</link>.

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
Get an item from the buffer. If no items are available, block until one becomes available.

** site BoundedBuffer<A>.getnb() :: A
Get an item from the buffer. If no items are available, halt.

** site BoundedBuffer<A>.put(A) :: Signal
Put an item in the buffer. If no slots are open, block until one becomes open.

** site BoundedBuffer<A>.putnb(A) :: Signal
Put an item in the buffer. If no slots are open, halt.

** site BoundedBuffer<A>.close() :: Signal
Close the buffer. This has the effect of immediately causing any blocked calls
to <code>get</code> and <code>put</code> to halt. In addition, any subsequent
calls to <code>get</code> or <code>put</code> will halt.

** site BoundedBuffer<A>.isClosed() :: Boolean
If the buffer is currently closed, return true, otherwise return false.

** site BoundedBuffer<A>.getOpen() :: Integer
Return the number of open slots in the buffer. Because of concurrency
this value may become out-of-date so it should only be used for debugging
or statistical measurements.

** site BoundedBuffer<A>.getBound() :: Integer
Return the total number of slots (open or filled) in the buffer.
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
Create a rewritable storage location without an initial value.

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
* site Null() :: Bot
Return a Java null value. This is only necessary to interface with certain
Java libraries, Orc programs should use the <code>None()</code> constructor
instead of null values.
--}
site Null = orc.lib.util.JavaNull

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
Construct an available optional value.

** pattern Some(Option<A>) :: (A)
Deconstruct an available optional value.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 3
Some(3) >Some(x)> x]]></programlisting>
--}
site Some = orc.runtime.sites.core.Some

{--
* site None(A) :: Option<A>
Construct an unavailable optional value.

** pattern None(Option<A>) :: ()
Deconstruct an unavailable optional value.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: true
  None() >None()> true
| Some(3) >None()> false]]></programlisting>
--}
site None = orc.runtime.sites.core.None

{--
* site Left(A) :: Either<A>
Construct a "left" member of a union which may be tagged either "left" or "right".

** pattern Left(Either<A>) :: A
Deconstruct a "left" member of a union.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "left"
Left(3) >x> (
   x >Right(_)> "right"
 | x >Left(_)> "left"
)]]></programlisting>
--}
site Left = orc.lib.data.Left

{--
* site Right(A) :: Either<A>
Construct a "right" member of a union which may be tagged either "left" or "right".

** pattern Right(Either<A>) :: A
Deconstruct a "right" member of a union.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "right"
Right(3) >x> (
   x >Right(_)> "right"
 | x >Left(_)> "left"
)]]></programlisting>
--}
site Right = orc.lib.data.Right

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

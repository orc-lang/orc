{--
General-purpose supplemental data structures.
--}

{--
* site Semaphore(Integer) :: Semaphore
Return a semaphore with the given number of available items.

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
Acquire an item. If none are available, block until one becomes available.

** site Semaphore.acquirenb() :: Signal
Acquire an item. If no items are available, fail silently.

** site Semaphore.release() :: Signal
Release an item.  This may be called to add items beyond
the initial number of available items with which the semaphore was constructed.
<<<<<<< data.inc

* site Semaphore.acquirenb() :: Signal
Acquire an item. If none are available, halt. 

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

** site Buffer<A>.getnb() :: A
Get an item from the buffer. If no items are available, halt.

** site Buffer<A>.put(A) :: Signal
Put an item in the buffer.

** site Buffer<A>.close() :: Signal
Close the buffer. This has the effect of immediately causing any blocked calls
to <code>get</code> to halt. In addition, any subsequent calls to
<code>get</code> or <code>put</code> will halt.
--}
site Buffer = orc.lib.state.Buffer

{--
* site SyncChannel() :: SyncChannel<A>
Create a synchronous channel, or rendezvous.

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
Create an empty rewritable storage location.

Example:
<programlisting language="orc-demo"><![CDATA[
val r = Ref()
Rtimer(1000) >> r.write(5) >> stop
| println(r.read()) >>
  r.write(10) >>
  println(r.read()) >>
  stop]]></programlisting>

** site Ref<A>(A) :: Ref<A>
Create a rewritable storage location containing the provided value.

** site Ref<A>.read() :: A
Read the value of the ref. If the ref is empty, block until it is no longer empty.

** site Ref<A>.readnb() :: A
Read the value of the ref. If the ref is empty, fail silently.

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
Create a new array of the given size.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 1 2
val a = Array(3)
for(0, a.length()) >i>
a.set(i, f(i)) >>
stop
; a.get(0) | a.get(1) | a.get(2)]]></programlisting>

** site Array<A>(Integer, String) :: Array<A>
Create a new primitive array of the given size with the given primitive type.
The primitive type should match the element type of the array, although a
typechecker may not be able to verify this. This constructor is only necessary
when interfacing with certain Java libraries; most programs will just use the 
<code>Array&lt;A&gt;(Integer)</code> constructor.

** site Array<A>.get(Integer) :: A
Get the element of the array given by the index, counting from 0.

** site Array<A>.set(Integer, A) :: Signal
Set the element of the array given by the index, counting from 0.

** site Array<A>.slice(Integer, Integer) :: Array<A>
Return a copy of the portion of the array with indices covered by the given
half-open range. The result array is still indexed counting from 0.

** site Array<A>.length() :: Integer
Return the size of the array.
--}
site Array = orc.lib.util.JavaArray

{--
* def fillArray(Array<A>, lambda (Integer) :: A) :: Array<A>
Given an array and a function from indices to values, populate the array.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 1 2 3
val a = fillArray(Array(3), let)
a.get(0) | a.get(1) | a.get(2)]]></programlisting>
--}
def fillArray(a, f) =
  val n = a.length()
  def fill(i, f) =
    if i = n then stop
    else ( a.set(i, f(i)) >> stop
         | fill(i+1, f) )
  fill(0, f) ; a
  
{--
* def IArray(Integer, lambda (Integer) :: A)(Integer) :: A
Create an immutable mapping (array) from integers to values.
The first argument gives the upper exclusive bound of the mapping
(the lower inclusive bound is always 0). The second argument
is a function which gives the value of the mapping for each index in the array.

You can think of the immutable mapping as a memoized version of the
function over a finite range.
--}
def IArray(n, f) =
  fillArray(Array(n), f) >a>
  a.get

{--
* site Some(A) :: Option<A>
Construct an available optional value.

* pattern Some(Option<A>) :: (A)
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

* pattern None(Option<A>) :: ()
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

* pattern Left(Either<A>) :: A
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

* pattern Right(Either<A>) :: A
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

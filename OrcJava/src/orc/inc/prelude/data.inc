{--
site Semaphore(Integer) :: Semaphore
Construct a semaphore with the given number of available items.

site Semaphore.acquire() :: Signal
Acquire an item. If none are available, block until one becomes available.

site Semaphore.release() :: Signal
Release an item.  This may be called to add items beyond
the initial number of available items with which the semaphore was constructed.
--}
site Semaphore = orc.lib.state.Semaphore

{--
site Buffer<A>() :: Buffer<A>
Create a new buffer (FIFO channel) of unlimited size.

site Buffer<A>.get() :: A
Get an item from the buffer. If no items are available, block until one becomes available.

site Buffer<A>.getnb() :: A
Get an item from the buffer. If no items are available, fail silently.

site Buffer<A>.put(A) :: Signal
Put an item in the buffer.

site Buffer<A>.close() :: Signal
Close the buffer. This has the effect of immediately causing any blocked calls
to <code>get</code> to fail silently. In addition, any subsequent calls to
<code>get</code> or <code>put</code> will fail silently.
--}
site Buffer = orc.lib.state.Buffer

{--
site SyncChannel<A>() :: SyncChannel<A>
Create a synchronous channel.

site SyncChannel<A>.get() :: A
Receive an item over the channel. If no sender is available, block until one becomes available.

site SyncChannel<A>.put(A) :: Signal
Send an item over the channel. If no receiver is available, block until one becomes available.
--}
site SyncChannel = orc.lib.state.SyncChannel

{--
site Cell<A>() :: Cell<A>
Create a write-once cell.

site Cell<A>.read() :: A
Read a value from the cell. If the cell does not yet have a value, block until it receives one.

site Cell<A>.write() :: Signal
Write a value to the cell. If the cell already has a value, fail silently.
--}
site Cell = orc.lib.state.Cell

{--
site Ref<A>() :: Ref<A>
site Ref<A>(A) :: Ref<A>
Create a rewritable reference. If no argument is provided, the
reference will be considered empty, otherwise it will be initialized
with the provided value.

site Ref<A>.read() :: A
Read the value of the reference. If the reference is empty, block until it is no longer empty.

site Ref<A>.write() :: Signal
Write a value to the reference.
--}
site Ref = orc.lib.state.Ref

{--
site Null() :: Bot
Return a Java null value.
--}
site Null = orc.lib.util.JavaNull

{--
site Array<A>(Integer, String) :: Array<A>
Create a new primitive array of the given size with the given primitive type.
The primitive type should match the element type of the array, although a
typechecker may not be able to verify this.

site Array<A>(Integer) :: Array<A>
Create a new array of the given size.

site Array<A>.get(Integer) :: A
Get an element of the array, counting from 0.

site Array<A>.set(Integer, A) :: Signal
Set an element of the array, counting from 0.

site Array<A>.slice(Integer, Integer) :: Array<A>
Return a copy of a portion of the array with indices covered by the given
half-open range. The result array is still indexed counting from 0.
--}
site Array = orc.lib.util.JavaArray

{--
site Some<A>(A) :: Option<A>
Construct an available optional value.

pattern Some<A>(Option<A>) :: (A)
Deconstruct an available optional value.
--}
site Some = orc.runtime.sites.core.Some

{--
site None<A>(A) :: Option<A>
Construct an unavailable optional value.

pattern None<A>(Option<A>) :: ()
Deconstruct an unavailable optional value.
--}
site None = orc.runtime.sites.core.None

{--
site Left<A>(A) :: Either<A>
Construct a "left" member of a union.

pattern Left<A>(Either<A>) :: ()
Deconstruct a "left" member of a union.
--}
site Left = orc.lib.data.Left

{--
site Right<A>(A) :: Either<A>
Construct a "right" member of a union.

pattern Right<A>(Either<A>) :: ()
Deconstruct a "right" member of a union.
--}
site Right = orc.lib.data.Right

{--
def fst<A,B>(A,B) :: A
Return the first element of a 2-tuple.
--}
def fst((x,_)) = x

{--
def snd<A,B>(A,B) :: B
Return the second element of a 2-tuple.
--}
def snd((_,y)) = y

{--
def swap<A,B>((A,B)) :: (B,A)
Swap the elements of a pair.
--}
def swap((x,y)) = (y,x)
